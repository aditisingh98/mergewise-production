package com.mergewise.review.normalize;

import com.mergewise.context.AgentContext;
import com.mergewise.dto.review.CanonicalFinding;
import com.mergewise.dto.review.ExplainedScore;
import com.mergewise.dto.review.ScoreDeduction;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ExplainedScoreBuilder {

    private static final Set<String> QUALITY_TABS = Set.of("MAINTAINABILITY", "ARCHITECTURE");
    private static final Set<String> SECURITY_TABS = Set.of("SECURITY");
    private static final Set<String> PERFORMANCE_TABS = Set.of("PERFORMANCE");
    private static final Set<String> MAINTAINABILITY_TABS = Set.of("MAINTAINABILITY");

    public ExplainedScore buildCategoryScore(
            List<CanonicalFinding> findings,
            Set<String> tabs,
            String label,
            String calculation) {

        int score = 100;
        Map<String, ScoreDeduction> deductionMap = new LinkedHashMap<>();
        List<String> improve = new ArrayList<>();

        for (CanonicalFinding finding : findings) {
            if (!tabs.contains(finding.getTab())) {
                continue;
            }
            int penalty = SeverityUtils.penalty(finding.getSeverity());
            score -= penalty;
            String key = finding.getSeverity() + " " + finding.getTab();
            deductionMap.merge(key, ScoreDeduction.builder()
                    .reason("1 " + finding.getSeverity() + " " + tabLabel(finding.getTab()))
                    .points(-penalty)
                    .build(), (a, b) -> ScoreDeduction.builder()
                    .reason(incrementReason(a.getReason()))
                    .points(a.getPoints() + b.getPoints())
                    .build());

            if (penalty >= 8 && improve.size() < 4) {
                improve.add(finding.getRecommendation() != null
                        ? finding.getRecommendation()
                        : "Address: " + finding.getTitle());
            }
        }

        score = clamp(score);
        if (improve.isEmpty() && score < 90) {
            improve.add("Resolve remaining " + label.toLowerCase() + " findings to improve score.");
        }

        return ExplainedScore.builder()
                .score(score)
                .grade(SeverityUtils.grade(score))
                .meaning(SeverityUtils.meaningForScore(score))
                .howCalculated(calculation)
                .deductions(new ArrayList<>(deductionMap.values()))
                .howToImprove(improve.stream().distinct().limit(5).toList())
                .build();
    }

    public ExplainedScore buildOverallScore(List<CanonicalFinding> findings) {
        return buildCategoryScore(
                findings,
                Set.of("SECURITY", "RUNTIME", "PERFORMANCE", "MAINTAINABILITY", "ARCHITECTURE", "TESTING"),
                "Overall",
                "Starts at 100. Deducts points by severity across all normalized findings.");
    }

    public ExplainedScore buildComplexityScore(AgentContext context) {
        int score = 100;
        List<ScoreDeduction> deductions = new ArrayList<>();
        List<String> improve = new ArrayList<>();

        int files = context.getFileChanges() != null ? context.getFileChanges().size() : 0;
        int totalChanges = context.getFileChanges() != null
                ? context.getFileChanges().stream()
                .mapToInt(f -> f.getChanges() != null ? f.getChanges() : 0)
                .sum()
                : 0;

        if (files > 20) {
            score -= 15;
            deductions.add(ScoreDeduction.builder().reason("Large PR (" + files + " files)").points(-15).build());
            improve.add("Split into smaller PRs to reduce review risk.");
        }
        if (totalChanges > 500) {
            score -= 20;
            deductions.add(ScoreDeduction.builder().reason("High line churn (" + totalChanges + " lines)").points(-20).build());
            improve.add("Reduce scope or split changes across releases.");
        }
        if ("CRITICAL".equals(context.getRiskLevel()) || "HIGH".equals(context.getRiskLevel())) {
            score -= 15;
            deductions.add(ScoreDeduction.builder()
                    .reason("Elevated planner risk: " + context.getRiskLevel())
                    .points(-15)
                    .build());
        }

        score = clamp(score);
        return ExplainedScore.builder()
                .score(score)
                .grade(SeverityUtils.grade(score))
                .meaning(SeverityUtils.meaningForScore(score))
                .howCalculated("Based on files changed, line churn, and planner risk level.")
                .deductions(deductions)
                .howToImprove(improve)
                .build();
    }

    public ExplainedScore buildMergeConfidence(List<CanonicalFinding> findings, AgentContext context) {
        int score = 100;
        List<ScoreDeduction> deductions = new ArrayList<>();
        List<String> improve = new ArrayList<>();

        long critical = countSeverity(findings, "CRITICAL");
        long high = countSeverity(findings, "HIGH");
        long medium = countSeverity(findings, "MEDIUM");

        if (critical > 0) {
            int pts = (int) (critical * 30);
            score -= pts;
            deductions.add(ScoreDeduction.builder().reason(critical + " Critical issue(s)").points(-pts).build());
            improve.add("Fix all critical findings before merge.");
        }
        if (high > 0) {
            int pts = (int) (high * 15);
            score -= pts;
            deductions.add(ScoreDeduction.builder().reason(high + " High issue(s)").points(-pts).build());
            improve.add("Resolve high-severity findings.");
        }
        if (medium > 0) {
            int pts = (int) (medium * 5);
            score -= pts;
            deductions.add(ScoreDeduction.builder().reason(medium + " Medium issue(s)").points(-pts).build());
        }
        if (context.getOverallScore() != null && context.getOverallScore() > 70) {
            score -= 10;
            deductions.add(ScoreDeduction.builder().reason("High planner risk score").points(-10).build());
        }

        score = clamp(score);
        return ExplainedScore.builder()
                .score(score)
                .grade(SeverityUtils.grade(score))
                .meaning("Confidence that this change set is safe to merge.")
                .howCalculated("Penalizes critical/high/medium findings and planner risk.")
                .deductions(deductions)
                .howToImprove(improve)
                .build();
    }

    private long countSeverity(List<CanonicalFinding> findings, String severity) {
        return findings.stream().filter(f -> severity.equals(f.getSeverity())).count();
    }

    private String incrementReason(String reason) {
        if (reason == null) {
            return "1 issue";
        }
        if (reason.startsWith("1 ")) {
            return "2" + reason.substring(1);
        }
        return reason;
    }

    private String tabLabel(String tab) {
        return tab != null ? tab.substring(0, 1) + tab.substring(1).toLowerCase() : "Issue";
    }

    private int clamp(int score) {
        return Math.max(0, Math.min(100, score));
    }
}
