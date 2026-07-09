package com.mergewise.review.pipeline;

import com.mergewise.context.AgentContext;
import com.mergewise.dto.review.DashboardView;
import com.mergewise.dto.review.ExplainedScore;
import com.mergewise.dto.review.ReviewIssueModel;
import com.mergewise.review.normalize.SeverityUtils;
import com.mergewise.service.MergeDecisionEngine;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardBuilder {

    public DashboardView build(
            AgentContext context,
            List<ReviewIssueModel> issues,
            MergeDecisionEngine.MergeDecision decision,
            ExplainedScore mergeConfidence) {

        Map<String, Integer> counts = new HashMap<>();
        counts.put("critical", count(issues, "CRITICAL"));
        counts.put("high", count(issues, "HIGH"));
        counts.put("medium", count(issues, "MEDIUM"));
        counts.put("low", count(issues, "LOW"));
        counts.put("info", count(issues, "INFO"));

        boolean canMerge = "APPROVE".equals(decision.getDecision())
                || "APPROVE_WITH_WARNINGS".equals(decision.getDecision());

        return DashboardView.builder()
                .canMerge(canMerge)
                .overallStatus(decision.getDecision())
                .riskLevel(context.getRiskLevel())
                .mergeConfidence(mergeConfidence.getScore())
                .filesChanged(context.getFileChanges() != null ? context.getFileChanges().size() : 0)
                .totalIssues(issues.size())
                .totalSuggestions((int) issues.stream().filter(i -> !"CRITICAL".equals(i.getSeverity())).count())
                .estimatedFixTime(estimateFixTime(issues))
                .issueCounts(counts)
                .build();
    }

    private int count(List<ReviewIssueModel> issues, String severity) {
        return (int) issues.stream().filter(i -> severity.equals(i.getSeverity())).count();
    }

    private String estimateFixTime(List<ReviewIssueModel> issues) {
        int minutes = issues.stream().mapToInt(i -> switch (SeverityUtils.normalize(i.getSeverity())) {
            case "CRITICAL" -> 240;
            case "HIGH" -> 120;
            case "MEDIUM" -> 60;
            case "LOW" -> 30;
            default -> 15;
        }).sum();
        if (minutes < 60) {
            return minutes + " minutes";
        }
        return (minutes / 60) + "h " + (minutes % 60) + "m";
    }
}
