package com.mergewise.review.diff;

import com.mergewise.dto.PRFileChange;
import com.mergewise.dto.review.FileDiffSummary;
import com.mergewise.dto.review.IssueTabItem;
import com.mergewise.dto.review.IssuesTabSection;
import com.mergewise.dto.review.ReviewIssueModel;
import com.mergewise.dto.review.SuggestionTabItem;
import com.mergewise.dto.review.SuggestionsTabSection;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class IssuesTabBuilder {

    private final CodeFixSuggester codeFixSuggester;

    public IssuesTabBuilder(CodeFixSuggester codeFixSuggester) {
        this.codeFixSuggester = codeFixSuggester;
    }

    public IssuesTabSection build(List<PRFileChange> fileChanges, List<ReviewIssueModel> issues) {
        List<ReviewIssueModel> safeIssues = issues != null ? issues : List.of();
        List<IssueTabItem> items = safeIssues.stream().map(this::toTabItem).toList();

        Map<String, List<String>> idsByFile = items.stream()
                .filter(i -> i.getFile() != null && !i.getFile().isBlank())
                .collect(Collectors.groupingBy(
                        IssueTabItem::getFile,
                        LinkedHashMap::new,
                        Collectors.mapping(IssueTabItem::getId, Collectors.toList())));

        List<FileDiffSummary> fileDiffs = new ArrayList<>();
        if (fileChanges != null) {
            for (PRFileChange change : fileChanges) {
                String file = change.getFilename();
                fileDiffs.add(FileDiffSummary.builder()
                        .file(file)
                        .status(change.getStatus())
                        .removedLines(copyLines(change.getRemovedLines()))
                        .addedLines(copyLines(change.getAddedLines()))
                        .issueIds(idsByFile.getOrDefault(file, List.of()))
                        .build());
            }
        }

        return IssuesTabSection.builder()
                .items(items)
                .fileDiffs(fileDiffs)
                .build();
    }

    public SuggestionsTabSection buildSuggestions(IssuesTabSection issuesTab) {
        if (issuesTab == null || issuesTab.getItems() == null) {
            return SuggestionsTabSection.builder().build();
        }
        List<SuggestionTabItem> suggestions = issuesTab.getItems().stream()
                .map(this::toSuggestion)
                .filter(s -> hasSuggestionContent(s))
                .toList();
        return SuggestionsTabSection.builder().items(suggestions).build();
    }

    private IssueTabItem toTabItem(ReviewIssueModel issue) {
        String issueCode = codeFixSuggester.resolveIssueCode(issue);
        String fixCode = codeFixSuggester.suggest(issue, issueCode);
        fixCode = distinctFix(fixCode, issueCode);

        String description = cleanDescription(issue.getDescription(), issue.getTitle());
        String fixGuide = firstNonBlank(issue.getRecommendation(), issue.getDevelopmentGuidance());

        return IssueTabItem.builder()
                .id(issue.getId())
                .severity(issue.getSeverity())
                .title(issue.getTitle())
                .description(description)
                .file(issue.getFile())
                .line(issue.getLine() != null && issue.getLine() > 0 ? issue.getLine() : null)
                .issueCode(issueCode)
                .fixCode(fixCode)
                .fixGuide(fixGuide)
                .build();
    }

    private SuggestionTabItem toSuggestion(IssueTabItem item) {
        return SuggestionTabItem.builder()
                .id(item.getId())
                .severity(item.getSeverity())
                .title(item.getTitle())
                .file(item.getFile())
                .line(item.getLine())
                .issueCode(item.getIssueCode())
                .fixCode(item.getFixCode())
                .fixGuide(item.getFixGuide())
                .build();
    }

    private boolean hasSuggestionContent(SuggestionTabItem item) {
        return (item.getFixCode() != null && !item.getFixCode().isBlank())
                || (item.getFixGuide() != null && !item.getFixGuide().isBlank());
    }

    private String distinctFix(String fixCode, String issueCode) {
        if (fixCode == null || issueCode == null) {
            return fixCode;
        }
        if (fixCode.trim().equals(issueCode.trim())) {
            return null;
        }
        return fixCode;
    }

    private String cleanDescription(String description, String title) {
        if (description == null || description.isBlank()) {
            return title;
        }
        int codeIdx = description.indexOf(" Code: `");
        if (codeIdx > 0) {
            return description.substring(0, codeIdx).trim();
        }
        return description;
    }

    private List<String> copyLines(List<String> lines) {
        return lines != null ? new ArrayList<>(lines) : new ArrayList<>();
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
