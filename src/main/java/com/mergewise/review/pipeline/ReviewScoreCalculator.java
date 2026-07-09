package com.mergewise.review.pipeline;

import com.mergewise.context.AgentContext;
import com.mergewise.dto.PRFileChange;
import com.mergewise.dto.ReviewIssue;
import com.mergewise.dto.review.ExplainedScore;
import com.mergewise.dto.review.ReviewIssueModel;
import com.mergewise.dto.review.ScoreDeduction;
import com.mergewise.review.normalize.SeverityUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ReviewScoreCalculator {

    public ExplainedScore overall(List<ReviewIssueModel> issues) {
        return build(issues, Set.of("SECURITY", "RUNTIME", "PERFORMANCE", "MAINTAINABILITY", "ARCHITECTURE", "TESTING"),
                "Overall code health score based on normalized findings only.");
    }

    public ExplainedScore quality(List<ReviewIssueModel> issues) {
        return build(issues, Set.of("MAINTAINABILITY", "ARCHITECTURE"),
                "Quality score from maintainability and architecture findings.");
    }

    public ExplainedScore security(List<ReviewIssueModel> issues) {
        return build(issues, Set.of("SECURITY"), "Security score from genuine security findings only.");
    }

    public ExplainedScore performance(List<ReviewIssueModel> issues) {
        return build(issues, Set.of("PERFORMANCE"), "Performance score from performance findings.");
    }

    public ExplainedScore maintainability(List<ReviewIssueModel> issues) {
        return build(issues, Set.of("MAINTAINABILITY"), "Maintainability score from code quality findings.");
    }

    public ExplainedScore complexity(AgentContext context) {
        int score = 100;
        List<ScoreDeduction> deductions = new ArrayList<>();
        List<String> improve = new ArrayList<>();

        int files = context.getFileChanges() != null ? context.getFileChanges().size() : 0;
        int totalChanges = context.getFileChanges() != null
                ? context.getFileChanges().stream().mapToInt(f -> f.getChanges() != null ? f.getChanges() : 0).sum()
                : 0;

        if (files > 20) {
            score -= 15;
            deductions.add(ScoreDeduction.builder().reason("Large PR (" + files + " files)").points(-15).build());
            improve.add("Split into smaller PRs.");
        }
        if (totalChanges > 500) {
            score -= 20;
            deductions.add(ScoreDeduction.builder().reason("High line churn").points(-20).build());
        }
        if ("CRITICAL".equals(context.getRiskLevel()) || "HIGH".equals(context.getRiskLevel())) {
            score -= 15;
            deductions.add(ScoreDeduction.builder().reason("Elevated planner risk").points(-15).build());
        }

        score = clamp(score);
        return ExplainedScore.builder()
                .score(score)
                .grade(SeverityUtils.grade(score))
                .meaning(SeverityUtils.meaningForScore(score))
                .howCalculated("Based on change size and planner risk profile.")
                .deductions(deductions)
                .howToImprove(improve)
                .build();
    }

    public ExplainedScore mergeConfidence(List<ReviewIssueModel> issues, AgentContext context) {
        int score = 100;
        List<ScoreDeduction> deductions = new ArrayList<>();
        List<String> improve = new ArrayList<>();

        long critical = count(issues, "CRITICAL");
        long high = count(issues, "HIGH");
        long medium = count(issues, "MEDIUM");

        if (critical > 0) {
            int pts = (int) (critical * 30);
            score -= pts;
            deductions.add(ScoreDeduction.builder().reason(critical + " critical").points(-pts).build());
            improve.add("Fix critical issues before merge.");
        }
        if (high > 0) {
            int pts = (int) (high * 15);
            score -= pts;
            deductions.add(ScoreDeduction.builder().reason(high + " high").points(-pts).build());
        }
        if (medium > 0) {
            int pts = (int) (medium * 5);
            score -= pts;
            deductions.add(ScoreDeduction.builder().reason(medium + " medium").points(-pts).build());
        }

        score = clamp(score);
        return ExplainedScore.builder()
                .score(score)
                .grade(SeverityUtils.grade(score))
                .meaning("Confidence that this PR is safe to merge.")
                .howCalculated("Penalizes severity-weighted findings. Infrastructure failures are excluded.")
                .deductions(deductions)
                .howToImprove(improve)
                .build();
    }

    private ExplainedScore build(List<ReviewIssueModel> issues, Set<String> categories, String calculation) {
        int score = 100;
        Map<String, ScoreDeduction> deductions = new LinkedHashMap<>();
        List<String> improve = new ArrayList<>();

        for (ReviewIssueModel issue : issues) {
            if (!categories.contains(issue.getExplorerCategory())) {
                continue;
            }
            int penalty = SeverityUtils.penalty(issue.getSeverity());
            score -= penalty;
            String key = issue.getSeverity() + ":" + issue.getExplorerCategory();
            deductions.merge(key,
                    ScoreDeduction.builder().reason("1 " + issue.getSeverity() + " finding").points(-penalty).build(),
                    (a, b) -> ScoreDeduction.builder().reason(a.getReason()).points(a.getPoints() + b.getPoints()).build());
            if (penalty >= 8 && improve.size() < 4 && issue.getRecommendation() != null) {
                improve.add(issue.getRecommendation());
            }
        }

        score = clamp(score);
        return ExplainedScore.builder()
                .score(score)
                .grade(SeverityUtils.grade(score))
                .meaning(SeverityUtils.meaningForScore(score))
                .howCalculated(calculation)
                .deductions(new ArrayList<>(deductions.values()))
                .howToImprove(improve)
                .build();
    }

    private long count(List<ReviewIssueModel> issues, String severity) {
        return issues.stream().filter(i -> severity.equals(i.getSeverity())).count();
    }

    private int clamp(int score) {
        return Math.max(0, Math.min(100, score));
    }
}
