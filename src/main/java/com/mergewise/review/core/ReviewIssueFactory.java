package com.mergewise.review.core;

import com.mergewise.dto.ReviewIssue;

import java.util.UUID;

public final class ReviewIssueFactory {

    private ReviewIssueFactory() {
    }

    public static ReviewIssue issue(
            String category,
            String severity,
            String file,
            int line,
            String title,
            String rootCause,
            String description,
            String productionImpact,
            String fixRecommendation,
            String fixedCodeExample,
            int confidenceScore) {

        return ReviewIssue.builder()
                .id(UUID.randomUUID().toString())
                .category(category)
                .severity(severity)
                .file(file)
                .line(line)
                .title(title)
                .rootCause(rootCause)
                .description(description)
                .productionImpact(productionImpact)
                .fixRecommendation(fixRecommendation)
                .fixedCodeExample(fixedCodeExample)
                .confidenceScore(confidenceScore)
                .build();
    }
}
