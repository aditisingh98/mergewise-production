package com.mergewise.service;

import com.mergewise.context.AgentContext;
import com.mergewise.dto.ReviewIssue;
import com.mergewise.dto.review.ReviewIssueModel;
import com.mergewise.review.pipeline.IssueDeduplicator;
import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MergeDecisionEngine {

    private final IssueDeduplicator issueDeduplicator;

    public MergeDecisionEngine(IssueDeduplicator issueDeduplicator) {
        this.issueDeduplicator = issueDeduplicator;
    }

    public MergeDecision decide(AgentContext context) {
        return decide(issueDeduplicator.deduplicate(context.getReviewIssues()).getIssues());
    }

    public MergeDecision decide(List<ReviewIssueModel> issues) {
        long critical = count(issues, "CRITICAL");
        long high = count(issues, "HIGH");
        long medium = count(issues, "MEDIUM");
        long low = count(issues, "LOW");

        String decision;
        String reasoning;

        if (critical > 0) {
            decision = "BLOCK_MERGE";
            reasoning = String.format(
                    "Blocked: %d critical issue(s) threaten production stability, security, or data integrity.",
                    critical);
        } else if (high > 0) {
            decision = "NEEDS_CHANGES";
            reasoning = String.format(
                    "%d high-severity issue(s) must be fixed. Additionally %d medium and %d low finding(s) remain.",
                    high, medium, low);
        } else if (medium > 0 || low > 0) {
            decision = "APPROVE_WITH_WARNINGS";
            reasoning = String.format(
                    "No blocking issues. %d medium and %d low finding(s) can be addressed after merge.",
                    medium, low);
        } else {
            decision = "APPROVE";
            reasoning = "No code issues detected. Safe to merge from an automated review perspective.";
        }

        List<String> blockingIds = issues.stream()
                .filter(i -> Boolean.TRUE.equals(i.getBlocking()))
                .map(ReviewIssueModel::getId)
                .collect(Collectors.toList());

        return MergeDecision.builder()
                .decision(decision)
                .reasoning(reasoning)
                .criticalCount((int) critical)
                .highCount((int) high)
                .mediumCount((int) medium)
                .lowCount((int) low)
                .blockingIssueIds(blockingIds)
                .build();
    }

    private long count(List<ReviewIssueModel> issues, String severity) {
        return issues.stream().filter(i -> severity.equalsIgnoreCase(i.getSeverity())).count();
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
        private List<String> blockingIssueIds;
    }
}
