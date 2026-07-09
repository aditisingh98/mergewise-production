package com.mergewise.review.pipeline;

import com.mergewise.context.AgentContext;
import com.mergewise.dto.ReviewIssue;
import com.mergewise.service.HeuristicReviewService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class StaticAnalysisService {

    private static final Pattern HEURISTIC_ISSUE_PATTERN = Pattern.compile(
            "^\\[(critical|high|medium|low)]\\s+(.+?)\\s+-\\s+(.+)$",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private final HeuristicReviewService heuristicReviewService;

    public StaticAnalysisService(HeuristicReviewService heuristicReviewService) {
        this.heuristicReviewService = heuristicReviewService;
    }

    public List<ReviewIssue> analyze(AgentContext context) {
        HeuristicReviewService.Review review = heuristicReviewService.review(context.getFileChanges());
        List<ReviewIssue> issues = new ArrayList<>();
        List<String> issueLines = review.getIssues();
        List<String> suggestionLines = review.getSuggestions();

        for (int i = 0; i < issueLines.size(); i++) {
            String suggestion = i < suggestionLines.size() ? suggestionLines.get(i) : "";
            issues.add(fromHeuristicLine(issueLines.get(i), suggestion));
        }
        return issues;
    }

    private ReviewIssue fromHeuristicLine(String issueLine, String suggestion) {
        Matcher m = HEURISTIC_ISSUE_PATTERN.matcher(issueLine.trim());
        String severity = "MEDIUM";
        String file = "—";
        String description = issueLine;
        if (m.matches()) {
            severity = m.group(1).toUpperCase();
            file = m.group(2).trim();
            description = m.group(3).trim();
        }
        return ReviewIssue.builder()
                .id(UUID.randomUUID().toString())
                .severity(severity)
                .category("STATIC_ANALYSIS")
                .file(file)
                .line(0)
                .title(truncate(description, 120))
                .rootCause(description)
                .description(issueLine)
                .productionImpact("Static analysis signal; validate in context before merge.")
                .fixRecommendation(suggestion != null && !suggestion.isBlank()
                        ? suggestion
                        : "Address the finding using project conventions.")
                .confidenceScore(70)
                .build();
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max) + "...";
    }
}
