package com.mergewise.review.pipeline;

import com.mergewise.dto.review.IssueCategoryView;
import com.mergewise.dto.review.IssueExplorerSection;
import com.mergewise.dto.review.ReviewIssueModel;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class IssueCategorizer {

    public IssueExplorerSection categorize(List<ReviewIssueModel> issues) {
        return IssueExplorerSection.builder()
                .security(categoryView(issues, "SECURITY", "Security findings from static and semantic analysis."))
                .runtime(categoryView(issues, "RUNTIME", "Runtime risks including null safety and resource handling."))
                .performance(categoryView(issues, "PERFORMANCE", "Performance anti-patterns and scalability concerns."))
                .maintainability(categoryView(issues, "MAINTAINABILITY", "Code quality, readability, and maintainability improvements."))
                .testing(categoryView(issues, "TESTING", "Missing tests and coverage gaps."))
                .architecture(categoryView(issues, "ARCHITECTURE", "Architecture and design improvements."))
                .build();
    }

    private IssueCategoryView categoryView(List<ReviewIssueModel> issues, String category, String emptySummary) {
        List<String> ids = issues.stream()
                .filter(i -> category.equalsIgnoreCase(i.getExplorerCategory()))
                .map(ReviewIssueModel::getId)
                .collect(Collectors.toList());

        String summary = ids.isEmpty()
                ? "No " + category.toLowerCase() + " issues detected."
                : ids.size() + " " + category.toLowerCase() + " issue(s) found.";

        if (!ids.isEmpty()) {
            summary = ids.size() + " " + category.toLowerCase() + " issue(s) require attention.";
        }

        return IssueCategoryView.builder()
                .summary(summary)
                .count(ids.size())
                .issueIds(ids)
                .build();
    }
}
