package com.mergewise.review.diff;

import com.mergewise.dto.PRFileChange;
import com.mergewise.dto.review.FileChangeReview;
import com.mergewise.dto.review.ReviewIssueModel;
import com.mergewise.dto.review.SuggestionItem;
import com.mergewise.review.normalize.SeverityUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class FileChangeReviewBuilder {

    public List<FileChangeReview> build(List<PRFileChange> fileChanges, List<ReviewIssueModel> allIssues) {
        if (fileChanges == null || fileChanges.isEmpty()) {
            return List.of();
        }

        Map<String, List<ReviewIssueModel>> byFile = allIssues == null ? Map.of()
                : allIssues.stream()
                        .filter(i -> i.getFile() != null && !i.getFile().isBlank())
                        .collect(Collectors.groupingBy(ReviewIssueModel::getFile, LinkedHashMap::new, Collectors.toList()));

        List<FileChangeReview> result = new ArrayList<>();
        for (PRFileChange change : fileChanges) {
            String file = change.getFilename();
            List<ReviewIssueModel> fileIssues = byFile.getOrDefault(file, List.of());
            int score = fileScore(fileIssues, change);

            List<String> removed = change.getRemovedLines() != null
                    ? change.getRemovedLines()
                    : List.of();
            List<String> added = change.getAddedLines() != null
                    ? change.getAddedLines()
                    : List.of();

            result.add(FileChangeReview.builder()
                    .file(file)
                    .previousFilename(change.getPreviousFilename())
                    .status(change.getStatus())
                    .additions(change.getAdditions())
                    .deletions(change.getDeletions())
                    .changes(change.getChanges())
                    .language(change.getLanguage())
                    .module(change.getModule())
                    .backendCritical(change.getBackendCritical())
                    .riskScore(change.getRiskScore())
                    .patch(change.getPatch())
                    .removedLines(new ArrayList<>(removed))
                    .addedLines(new ArrayList<>(added))
                    .diffLines(new ArrayList<>())
                    .fileScore(score)
                    .risk(fileRisk(score))
                    .summary(buildSummary(fileIssues, change, removed.size(), added.size()))
                    .issues(new ArrayList<>(fileIssues))
                    .issueIds(fileIssues.stream().map(ReviewIssueModel::getId).toList())
                    .suggestions(toSuggestions(fileIssues))
                    .build());
        }
        return result;
    }

    private String buildSummary(
            List<ReviewIssueModel> issues,
            PRFileChange change,
            int removedCount,
            int addedCount) {

        if (issues.isEmpty()) {
            return String.format(
                    "No issues detected. %d line(s) removed, %d line(s) added (%s).",
                    removedCount,
                    addedCount,
                    change.getStatus() != null ? change.getStatus() : "modified");
        }
        return String.format(
                "%d finding(s). Old: %d removed line(s), new: %d added line(s), %d total changed lines.",
                issues.size(),
                removedCount,
                addedCount,
                change.getChanges() != null ? change.getChanges() : removedCount + addedCount);
    }

    private List<SuggestionItem> toSuggestions(List<ReviewIssueModel> issues) {
        List<SuggestionItem> suggestions = new ArrayList<>();
        for (ReviewIssueModel issue : issues) {
            if ((issue.getRecommendation() == null || issue.getRecommendation().isBlank())
                    && (issue.getDevelopmentGuidance() == null || issue.getDevelopmentGuidance().isBlank())
                    && (issue.getFixCode() == null || issue.getFixCode().isBlank())) {
                continue;
            }
            String guidance = firstNonBlank(issue.getDevelopmentGuidance(), issue.getRecommendation());
            suggestions.add(SuggestionItem.builder()
                    .id(issue.getId())
                    .title(issue.getTitle())
                    .description(guidance != null ? guidance : issue.getDescription())
                    .why(issue.getRootCause() != null ? issue.getRootCause() : issue.getDescription())
                    .expectedBenefit(issue.getProductionImpact())
                    .estimatedEffort(effortForSeverity(issue.getSeverity()))
                    .priority(issue.getSeverity())
                    .file(issue.getFile())
                    .line(issue.getLine())
                    .affectedCode(firstNonBlank(issue.getIssueCode(), issue.getAffectedCode(), issue.getNewCode()))
                    .oldCode(issue.getOldCode())
                    .newCode(firstNonBlank(issue.getIssueCode(), issue.getNewCode()))
                    .suggestedCode(firstNonBlank(issue.getFixCode(), issue.getFixedExample()))
                    .build());
        }
        return suggestions;
    }

    private int fileScore(List<ReviewIssueModel> issues, PRFileChange change) {
        int score = 100;
        for (ReviewIssueModel issue : issues) {
            score -= SeverityUtils.penalty(issue.getSeverity());
        }
        if (Boolean.TRUE.equals(change.getBackendCritical())) {
            score -= 5;
        }
        return Math.max(0, Math.min(100, score));
    }

    private String fileRisk(int score) {
        if (score >= 80) {
            return "LOW";
        }
        if (score >= 60) {
            return "MEDIUM";
        }
        if (score >= 40) {
            return "HIGH";
        }
        return "CRITICAL";
    }

    private String effortForSeverity(String severity) {
        return switch (SeverityUtils.normalize(severity)) {
            case "CRITICAL" -> "4h+";
            case "HIGH" -> "2h";
            case "MEDIUM" -> "1h";
            case "LOW" -> "30m";
            default -> "15m";
        };
    }

    private String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }
}
