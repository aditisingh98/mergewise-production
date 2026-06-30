package com.mergewise.service;

import com.mergewise.context.AgentContext;
import com.mergewise.dto.ReviewIssue;
import com.mergewise.dto.ScoreBreakdown;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class ScoreCalculatorService {

    private static final Set<String> QUALITY_CATEGORIES = Set.of(
            "CODE_QUALITY", "STATIC_ANALYSIS", "LOGGING", "QUALITY", "ARCHITECTURE");
    private static final Set<String> SECURITY_CATEGORIES = Set.of("SECURITY");
    private static final Set<String> PERFORMANCE_CATEGORIES = Set.of("PERFORMANCE");
    private static final Set<String> MAINTAINABILITY_CATEGORIES = Set.of(
            "CODE_QUALITY", "ARCHITECTURE", "STATIC_ANALYSIS");

    public ScoreBreakdown qualityScore(List<ReviewIssue> issues) {
        return buildScore(issues, QUALITY_CATEGORIES, "Code quality");
    }

    public ScoreBreakdown securityScore(List<ReviewIssue> issues) {
        return buildScore(issues, SECURITY_CATEGORIES, "Security posture");
    }

    public ScoreBreakdown performanceScore(List<ReviewIssue> issues) {
        return buildScore(issues, PERFORMANCE_CATEGORIES, "Performance");
    }

    public ScoreBreakdown maintainabilityScore(List<ReviewIssue> issues) {
        return buildScore(issues, MAINTAINABILITY_CATEGORIES, "Maintainability");
    }

    public ScoreBreakdown complexityScore(AgentContext context) {
        int score = 100;
        List<String> highlights = new ArrayList<>();

        int files = context.getFileChanges() != null ? context.getFileChanges().size() : 0;
        int totalChanges = context.getFileChanges() != null
                ? context.getFileChanges().stream()
                .mapToInt(f -> f.getChanges() != null ? f.getChanges() : 0)
                .sum()
                : 0;

        if (files > 20) {
            score -= 15;
            highlights.add("Large number of files changed (" + files + ")");
        }
        if (totalChanges > 500) {
            score -= 20;
            highlights.add("High line churn (" + totalChanges + " lines)");
        }
        if ("CRITICAL".equals(context.getRiskLevel()) || "HIGH".equals(context.getRiskLevel())) {
            score -= 15;
            highlights.add("Elevated planner risk level: " + context.getRiskLevel());
        }

        score = clamp(score);
        return ScoreBreakdown.builder()
                .score(score)
                .grade(grade(score))
                .summary("Complexity score based on change size and risk profile")
                .highlights(highlights)
                .build();
    }

    public ScoreBreakdown mergeConfidence(AgentContext context, List<ReviewIssue> issues) {
        int score = 100;
        List<String> highlights = new ArrayList<>();

        long critical = countSeverity(issues, "CRITICAL");
        long high = countSeverity(issues, "HIGH");
        long medium = countSeverity(issues, "MEDIUM");

        score -= (int) (critical * 30 + high * 15 + medium * 5);
        if (critical > 0) {
            highlights.add(critical + " critical issue(s) reduce merge confidence");
        }
        if (high > 0) {
            highlights.add(high + " high severity issue(s)");
        }
        if (context.getOverallScore() != null && context.getOverallScore() > 70) {
            score -= 10;
            highlights.add("High overall risk score from planner");
        }

        score = clamp(score);
        return ScoreBreakdown.builder()
                .score(score)
                .grade(grade(score))
                .summary("Confidence that this PR is safe to merge")
                .highlights(highlights)
                .build();
    }

    private ScoreBreakdown buildScore(List<ReviewIssue> issues, Set<String> categories, String label) {
        int score = 100;
        List<String> highlights = new ArrayList<>();

        for (ReviewIssue issue : issues) {
            if (issue.getCategory() == null || !categories.contains(issue.getCategory())) {
                continue;
            }
            int penalty = severityPenalty(issue.getSeverity());
            score -= penalty;
            if (penalty >= 15) {
                highlights.add(issue.getSeverity() + ": " + issue.getTitle());
            }
        }

        score = clamp(score);
        return ScoreBreakdown.builder()
                .score(score)
                .grade(grade(score))
                .summary(label + " score derived from detected findings")
                .highlights(highlights.stream().limit(5).toList())
                .build();
    }

    private int severityPenalty(String severity) {
        if (severity == null) {
            return 5;
        }
        return switch (severity.toUpperCase()) {
            case "CRITICAL" -> 25;
            case "HIGH" -> 15;
            case "MEDIUM" -> 8;
            case "LOW" -> 3;
            default -> 5;
        };
    }

    private long countSeverity(List<ReviewIssue> issues, String severity) {
        return issues.stream()
                .filter(i -> severity.equalsIgnoreCase(i.getSeverity()))
                .count();
    }

    private int clamp(int score) {
        return Math.max(0, Math.min(100, score));
    }

    private String grade(int score) {
        if (score >= 90) {
            return "A";
        }
        if (score >= 80) {
            return "B";
        }
        if (score >= 70) {
            return "C";
        }
        if (score >= 60) {
            return "D";
        }
        return "F";
    }
}
