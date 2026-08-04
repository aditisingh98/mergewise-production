package com.mergewise.review.pipeline;

import com.mergewise.dto.ReviewIssue;
import com.mergewise.service.AiReviewSupport;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AiReviewParser {

    private static final Pattern AI_ISSUE_PATTERN = Pattern.compile(
            "^ISSUE:\\s*\\[(critical|high|medium|low)]\\s+(.+?)\\s+-\\s+(.+)$",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern AI_ISSUE_LINE_PATTERN = Pattern.compile(
            "^ISSUE:\\s*\\[(critical|high|medium|low)]\\s+([^:]+):(\\d+)\\s+-\\s+(.+)$",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern AI_SUGGESTION_PATTERN = Pattern.compile(
            "^SUGGESTION:\\s*(.+)$",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern AI_LINE_PATTERN = Pattern.compile(
            "^LINE:\\s*(\\d+)\\s*$",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern AI_OLD_PATTERN = Pattern.compile(
            "^OLD:\\s*(.+)$",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern AI_NEW_PATTERN = Pattern.compile(
            "^NEW:\\s*(.+)$",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern AI_FIX_PATTERN = Pattern.compile(
            "^FIX:\\s*(.+)$",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    public List<ReviewIssue> parse(String analysis) {
        List<ReviewIssue> issues = new ArrayList<>();
        if (analysis == null || analysis.isBlank() || analysis.trim().equalsIgnoreCase("NO_ISSUES")) {
            return issues;
        }
        if (AiReviewSupport.isInfrastructureMessage(analysis)) {
            return issues;
        }

        ReviewIssue pending = null;
        for (String raw : analysis.split("\\R")) {
            String line = raw.trim();
            if (line.isBlank() || AiReviewSupport.isInfrastructureMessage(line)) {
                continue;
            }

            Matcher issueLineMatcher = AI_ISSUE_LINE_PATTERN.matcher(line);
            if (issueLineMatcher.matches()) {
                flush(issues, pending);
                pending = buildIssue(
                        issueLineMatcher.group(1),
                        issueLineMatcher.group(2).trim(),
                        Integer.parseInt(issueLineMatcher.group(3)),
                        issueLineMatcher.group(4).trim(),
                        line);
                continue;
            }

            Matcher issueMatcher = AI_ISSUE_PATTERN.matcher(line);
            if (issueMatcher.matches()) {
                flush(issues, pending);
                pending = buildIssue(
                        issueMatcher.group(1),
                        issueMatcher.group(2).trim(),
                        0,
                        issueMatcher.group(3).trim(),
                        line);
                if (pending == null) {
                    continue;
                }
                continue;
            }

            if (pending == null) {
                continue;
            }

            Matcher lineMatcher = AI_LINE_PATTERN.matcher(line);
            if (lineMatcher.matches()) {
                pending.setLine(Integer.parseInt(lineMatcher.group(1)));
                continue;
            }

            Matcher oldMatcher = AI_OLD_PATTERN.matcher(line);
            if (oldMatcher.matches()) {
                String oldCode = oldMatcher.group(1).trim();
                if (!"n/a".equalsIgnoreCase(oldCode)) {
                    pending.setRootCause(firstNonBlank(pending.getRootCause(), "") + "\nBefore: " + oldCode);
                }
                continue;
            }

            Matcher newMatcher = AI_NEW_PATTERN.matcher(line);
            if (newMatcher.matches()) {
                String newCode = newMatcher.group(1).trim();
                pending.setFixedCodeExample(newCode);
                continue;
            }

            Matcher fixMatcher = AI_FIX_PATTERN.matcher(line);
            if (fixMatcher.matches()) {
                pending.setFixedCodeExample(fixMatcher.group(1).trim());
                continue;
            }

            Matcher sugMatcher = AI_SUGGESTION_PATTERN.matcher(line);
            if (sugMatcher.matches()) {
                pending.setFixRecommendation(sugMatcher.group(1).trim());
                issues.add(pending);
                pending = null;
            }
        }

        if (pending != null) {
            if (pending.getFixRecommendation() == null || pending.getFixRecommendation().isBlank()) {
                pending.setFixRecommendation("Apply the suggested code change and add tests for the affected path.");
            }
            issues.add(pending);
        }
        return issues;
    }

    private ReviewIssue buildIssue(String severity, String file, int line, String problem, String rawLine) {
        if (AiReviewSupport.isInfrastructureIssue("AI_REVIEW", file, problem, rawLine)) {
            return null;
        }
        return ReviewIssue.builder()
                .id(UUID.randomUUID().toString())
                .severity(severity.toUpperCase())
                .category("AI_REVIEW")
                .file(file)
                .line(line)
                .title(truncate(problem, 120))
                .rootCause(problem)
                .description(problem)
                .productionImpact("AI-detected risk in changed code; verify before merge.")
                .fixRecommendation("")
                .confidenceScore(75)
                .build();
    }

    private void flush(List<ReviewIssue> issues, ReviewIssue pending) {
        if (pending != null) {
            if (pending.getFixRecommendation() == null || pending.getFixRecommendation().isBlank()) {
                pending.setFixRecommendation("Apply targeted changes per the issue description.");
            }
            issues.add(pending);
        }
    }

    private String appendField(String existing, String extra) {
        if (existing == null || existing.isBlank()) {
            return extra;
        }
        if (existing.contains(extra)) {
            return existing;
        }
        return existing + "\n" + extra;
    }

    private static String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        return fallback;
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max) + "...";
    }
}
