package com.mergewise.review.pipeline;

import com.mergewise.context.AgentContext;
import com.mergewise.dto.PRAnalysisResponse;
import com.mergewise.dto.review.ReviewSummaries;
import com.mergewise.service.AiReviewSupport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ReviewEngine {

    private final ReviewResponseBuilder reviewResponseBuilder;
    private final ProviderFallbackService providerFallbackService;

    @Value("${mergewise.ai.enabled:true}")
    private boolean aiEnabled;

    public ReviewEngine(
            ReviewResponseBuilder reviewResponseBuilder,
            ProviderFallbackService providerFallbackService) {
        this.reviewResponseBuilder = reviewResponseBuilder;
        this.providerFallbackService = providerFallbackService;
    }

    public PRAnalysisResponse buildResponse(AgentContext context) {
        ReviewSummaries summaries = buildSummaries(context);
        return reviewResponseBuilder.build(context, summaries);
    }

    private ReviewSummaries buildSummaries(AgentContext context) {
        String decision = context.getFinalDecision() != null ? context.getFinalDecision() : "UNKNOWN";
        int files = context.getFileChanges() != null ? context.getFileChanges().size() : 0;
        int issues = context.getReviewIssues() != null ? context.getReviewIssues().size() : 0;

        String executive = null;
        if (aiEnabled) {
            ProviderFallbackService.AiReviewAttempt attempt =
                    providerFallbackService.summarize(context, decision, false);
            if (attempt.getAnalysis() != null && !AiReviewSupport.isInfrastructureMessage(attempt.getAnalysis())) {
                executive = attempt.getAnalysis();
            }
            if (attempt.getStatus() != null) {
                context.getMetadata().put("aiSummaryStatusObject", attempt.getStatus());
            }
        }

        if (executive == null) {
            executive = String.format(
                    "PR %s#%d: %d file(s), %d finding(s). Recommendation: %s. Review completed using static analysis engines.",
                    context.getRepo(), context.getPrNumber(), files, issues, decision);
        }

        return ReviewSummaries.builder()
                .executive(executive)
                .developer(String.format(
                        "Start with fixFirst.topIssueIds. %d file(s) changed. Decision: %s.",
                        files, decision))
                .reviewer(String.format(
                        "Automated review: %s. Address blocking issues before approving.", decision))
                .releaseManager(switch (decision) {
                    case "APPROVE" -> "Low release risk. Standard deployment checks apply.";
                    case "APPROVE_WITH_WARNINGS" -> "Acceptable with documented follow-up.";
                    case "NEEDS_CHANGES" -> "Hold release until high-severity items are resolved.";
                    case "BLOCK_MERGE" -> "Do not release until critical defects are fixed.";
                    default -> context.getDecisionReasoning();
                })
                .build();
    }
}
