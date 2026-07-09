package com.mergewise.service;

import com.mergewise.context.AgentContext;
import com.mergewise.dto.ExecutiveSummary;
import com.mergewise.dto.ReviewIssue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AISummaryService {

    private final OpenAIService openAIService;

    public ExecutiveSummary buildSummary(AgentContext context, String decision, String reasoning) {
        List<ReviewIssue> issues = context.getReviewIssues() != null
                ? context.getReviewIssues()
                : List.of();

        int critical = (int) issues.stream().filter(i -> "CRITICAL".equalsIgnoreCase(i.getSeverity())).count();
        int high = (int) issues.stream().filter(i -> "HIGH".equalsIgnoreCase(i.getSeverity())).count();
        int filesChanged = context.getFileChanges() != null ? context.getFileChanges().size() : 0;

        String aiOverview = null;
        if (openAIService.isConfigured()) {
            try {
                aiOverview = openAIService.analyzeExecutiveSummary(context, decision);
                if (AiReviewSupport.isInfrastructureMessage(aiOverview)) {
                    context.getMetadata().put("aiSummaryStatus", "RATE_LIMITED");
                    context.getMetadata().put("aiSummaryMessage", aiOverview);
                    aiOverview = null;
                }
            } catch (AiProviderException ex) {
                log.warn("AI executive summary skipped ({}): {}", ex.getStatus(), ex.getMessage());
                context.getMetadata().put("aiSummaryStatus", ex.getStatus());
                context.getMetadata().put("aiSummaryMessage", ex.getMessage());
            } catch (Exception ex) {
                log.warn("AI executive summary failed: {}", ex.getMessage());
                context.getMetadata().put("aiSummaryStatus", "FAILED");
                context.getMetadata().put("aiSummaryMessage", ex.getMessage());
            }
        }

        String functionalImpact = inferFunctionalImpact(context, issues);
        String riskAssessment = String.format(
                "Risk level: %s. Overall score: %s. Findings: %d total (%d critical, %d high).",
                nullSafe(context.getRiskLevel()),
                nullSafe(context.getOverallScore()),
                issues.size(),
                critical,
                high);

        return ExecutiveSummary.builder()
                .overview(aiOverview != null ? aiOverview : buildFallbackOverview(context, issues, decision))
                .functionalImpact(functionalImpact)
                .riskAssessment(riskAssessment)
                .recommendedAction(reasoning)
                .totalIssues(issues.size())
                .criticalIssues(critical)
                .highIssues(high)
                .filesChanged(filesChanged)
                .build();
    }

    private String inferFunctionalImpact(AgentContext context, List<ReviewIssue> issues) {
        long functional = issues.stream().filter(i -> "FUNCTIONAL".equals(i.getCategory())).count();
        Object compat = context.getMetadata().get("backwardCompatibilityRisk");
        if (functional > 0 || compat != null) {
            return String.format(
                    "%d functional finding(s). Backward compatibility risk: %s.",
                    functional,
                    compat != null ? compat : "LOW");
        }
        return "No significant API or behavioral contract changes detected by static analysis.";
    }

    private String buildFallbackOverview(AgentContext context, List<ReviewIssue> issues, String decision) {
        return String.format(
                "PR %s#%d analyzed with %d findings. Merge recommendation: %s. "
                        + "Review covers functional, quality, security, performance, database, testing, and architecture dimensions.",
                nullSafe(context.getRepo()),
                context.getPrNumber(),
                issues.size(),
                decision);
    }

    private String nullSafe(Object value) {
        return value != null ? value.toString() : "N/A";
    }
}
