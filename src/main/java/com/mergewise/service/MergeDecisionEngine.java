package com.mergewise.service;

import com.mergewise.context.AgentContext;
import com.mergewise.dto.ReviewIssue;
import com.mergewise.dto.review.CanonicalFinding;
import com.mergewise.review.normalize.FindingNormalizer;
import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MergeDecisionEngine {

    private final FindingNormalizer findingNormalizer;

    public MergeDecisionEngine(FindingNormalizer findingNormalizer) {
        this.findingNormalizer = findingNormalizer;
    }

    public MergeDecision decide(AgentContext context) {
        List<ReviewIssue> raw = context.getReviewIssues() != null ? context.getReviewIssues() : List.of();
        return decide(findingNormalizer.normalize(raw).getFindings());
    }

    public MergeDecision decide(List<CanonicalFinding> findings) {
        long critical = count(findings, "CRITICAL");
        long high = count(findings, "HIGH");
        long medium = count(findings, "MEDIUM");
        long low = count(findings, "LOW");

        String decision;
        String reasoning;

        if (critical > 0) {
            decision = "BLOCK_MERGE";
            reasoning = String.format(
                    "Blocked due to %d critical issue(s) including security, runtime, or data-integrity risks.",
                    critical);
        } else if (high > 0) {
            decision = "NEEDS_CHANGES";
            reasoning = String.format(
                    "%d high-severity issue(s) require fixes. Medium/low findings: %d/%d.",
                    high, medium, low);
        } else if (medium > 0 || low > 0) {
            decision = "APPROVE_WITH_WARNINGS";
            reasoning = String.format(
                    "No critical or high issues. %d medium and %d low findings can merge with follow-up.",
                    medium, low);
        } else {
            decision = "APPROVE";
            reasoning = "No blocking or warning-level issues detected.";
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

    private long count(List<CanonicalFinding> findings, String severity) {
        return findings.stream().filter(i -> severity.equalsIgnoreCase(i.getSeverity())).count();
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
