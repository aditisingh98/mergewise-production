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

    private static final Pattern AI_SUGGESTION_PATTERN = Pattern.compile(
            "^SUGGESTION:\\s*(.+)$",
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

            Matcher issueMatcher = AI_ISSUE_PATTERN.matcher(line);
            if (issueMatcher.matches()) {
                if (pending != null) {
                    issues.add(pending);
                }
                String file = issueMatcher.group(2).trim();
                String problem = issueMatcher.group(3).trim();
                if (AiReviewSupport.isInfrastructureIssue("AI_REVIEW", file, problem, line)) {
                    pending = null;
                    continue;
                }
                pending = ReviewIssue.builder()
                        .id(UUID.randomUUID().toString())
                        .severity(issueMatcher.group(1).toUpperCase())
                        .category("AI_REVIEW")
                        .file(file)
                        .line(0)
                        .title(truncate(problem, 120))
                        .rootCause(problem)
                        .description(line)
                        .productionImpact("AI-detected risk in changed code.")
                        .fixRecommendation("")
                        .confidenceScore(75)
                        .build();
                continue;
            }

            Matcher sugMatcher = AI_SUGGESTION_PATTERN.matcher(line);
            if (sugMatcher.matches() && pending != null) {
                pending.setFixRecommendation(sugMatcher.group(1).trim());
                issues.add(pending);
                pending = null;
            }
        }

        if (pending != null) {
            if (pending.getFixRecommendation() == null || pending.getFixRecommendation().isBlank()) {
                pending.setFixRecommendation("Apply targeted changes per the issue description.");
            }
            issues.add(pending);
        }
        return issues;
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max) + "...";
    }
}
