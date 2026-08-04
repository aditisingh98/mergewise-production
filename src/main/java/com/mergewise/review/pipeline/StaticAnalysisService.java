package com.mergewise.review.pipeline;

import com.mergewise.context.AgentContext;
import com.mergewise.dto.ReviewIssue;
import com.mergewise.service.HeuristicReviewService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class StaticAnalysisService {

    private final HeuristicReviewService heuristicReviewService;

    public StaticAnalysisService(HeuristicReviewService heuristicReviewService) {
        this.heuristicReviewService = heuristicReviewService;
    }

    public List<ReviewIssue> analyze(AgentContext context) {
        HeuristicReviewService.Review review = heuristicReviewService.review(context.getFileChanges());
        List<ReviewIssue> issues = new ArrayList<>();

        for (HeuristicReviewService.HeuristicFinding finding : review.getFindings()) {
            issues.add(fromFinding(finding));
        }
        return issues;
    }

    private ReviewIssue fromFinding(HeuristicReviewService.HeuristicFinding finding) {
        String severity = finding.getSeverity() != null ? finding.getSeverity().toUpperCase() : "MEDIUM";
        String file = finding.getFile() != null ? finding.getFile() : "—";
        String message = finding.getMessage() != null ? finding.getMessage() : "Static analysis finding";

        return ReviewIssue.builder()
                .id(UUID.randomUUID().toString())
                .severity(severity)
                .category("STATIC_ANALYSIS")
                .file(file)
                .line(finding.getLine() > 0 ? finding.getLine() : 0)
                .title(truncate(message, 120))
                .rootCause(message)
                .description(finding.getMessage())
                .productionImpact("Static analysis signal in changed lines; validate in context before merge.")
                .fixRecommendation(finding.getSuggestion() != null && !finding.getSuggestion().isBlank()
                        ? finding.getSuggestion()
                        : "Address the finding using project conventions.")
                .confidenceScore(78)
                .build();
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max) + "...";
    }
}
