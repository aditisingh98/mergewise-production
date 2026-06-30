package com.mergewise.service;

import com.mergewise.context.AgentContext;
import com.mergewise.dto.ReviewIssue;
import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class MergeDecisionEngine {

    public MergeDecision decide(AgentContext context) {
        List<ReviewIssue> issues = context.getReviewIssues() != null
                ? context.getReviewIssues()
                : List.of();

        long critical = count(issues, "CRITICAL");
        long high = count(issues, "HIGH");
        long medium = count(issues, "MEDIUM");
        long low = count(issues, "LOW");

        String decision;
        String reasoning;

        if (critical > 0) {
            decision = "BLOCK_MERGE";
            reasoning = String.format(
                    "Blocked due to %d critical issue(s) including security, runtime, or data-integrity risks that must be resolved before production deployment.",
                    critical);
        } else if (high > 0) {
            decision = "NEEDS_CHANGES";
            reasoning = String.format(
                    "%d high-severity issue(s) require fixes. Medium/low findings: %d/%d. Address high-priority items and re-run analysis.",
                    high, medium, low);
        } else if (medium > 0 || low > 0) {
            decision = "APPROVE_WITH_WARNINGS";
            reasoning = String.format(
                    "No critical or high issues. %d medium and %d low findings can be merged with documented follow-up.",
                    medium, low);
        } else {
            decision = "APPROVE";
            reasoning = "No blocking or warning-level issues detected. PR meets automated review criteria.";
        }

        return MergeDecision.builder()
                .decision(decision)
                .reasoning(reasoning)
                .criticalCount((int) critical)
                .highCount((int) high)
                .mediumCount((int) medium)
                .lowCount((int) low)
                .build();
    }

    private long count(List<ReviewIssue> issues, String severity) {
        return issues.stream()
                .filter(i -> severity.equalsIgnoreCase(i.getSeverity()))
                .count();
    }

    @Data
    @Builder
    public static class MergeDecision {
        private String decision;
        private String reasoning;
        private int criticalCount;
        private int highCount;
        private int mediumCount;
        private int lowCount;
    }
}
