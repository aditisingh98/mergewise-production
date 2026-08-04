package com.mergewise.review.diff;

import com.mergewise.dto.PRFileChange;
import com.mergewise.dto.review.FileDiffSummary;
import com.mergewise.dto.review.IssueTabItem;
import com.mergewise.dto.review.IssuesTabSection;
import com.mergewise.dto.review.ReviewIssueModel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class IssuesTabBuilder {

    public IssuesTabSection build(List<PRFileChange> fileChanges, List<ReviewIssueModel> issues) {
        List<ReviewIssueModel> safeIssues = issues != null ? issues : List.of();
        List<IssueTabItem> items = safeIssues.stream().map(this::toTabItem).toList();

        Map<String, List<IssueTabItem>> issuesByFile = items.stream()
                .filter(i -> i.getFile() != null && !i.getFile().isBlank())
                .collect(Collectors.groupingBy(IssueTabItem::getFile, LinkedHashMap::new, Collectors.toList()));

        List<FileDiffSummary> fileDiffs = new ArrayList<>();
        if (fileChanges != null) {
            for (PRFileChange change : fileChanges) {
                String file = change.getFilename();
                fileDiffs.add(FileDiffSummary.builder()
                        .file(file)
                        .status(change.getStatus())
                        .removedLines(copyLines(change.getRemovedLines()))
                        .addedLines(copyLines(change.getAddedLines()))
                        .issues(issuesByFile.getOrDefault(file, List.of()))
                        .build());
            }
        }

        for (Map.Entry<String, List<IssueTabItem>> entry : issuesByFile.entrySet()) {
            boolean already = fileDiffs.stream().anyMatch(f -> entry.getKey().equals(f.getFile()));
            if (!already) {
                fileDiffs.add(FileDiffSummary.builder()
                        .file(entry.getKey())
                        .issues(entry.getValue())
                        .build());
            }
        }

        return IssuesTabSection.builder()
                .items(items)
                .fileDiffs(fileDiffs)
                .build();
    }

    private IssueTabItem toTabItem(ReviewIssueModel issue) {
        String issueCode = firstNonBlank(
                issue.getIssueCode(),
                issue.getNewCode(),
                issue.getAffectedCode(),
                issue.getOldCode());

        String fixCode = firstNonBlank(issue.getFixCode(), issue.getFixedExample());
        String fixGuide = firstNonBlank(
                issue.getRecommendation(),
                issue.getDevelopmentGuidance(),
                issue.getRootCause());

        return IssueTabItem.builder()
                .id(issue.getId())
                .severity(issue.getSeverity())
                .title(issue.getTitle())
                .description(issue.getDescription())
                .file(issue.getFile())
                .line(issue.getLine())
                .issueCode(issueCode)
                .fixCode(fixCode)
                .fixGuide(fixGuide)
                .build();
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
