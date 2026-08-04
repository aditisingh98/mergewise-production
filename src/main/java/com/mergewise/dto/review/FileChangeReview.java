package com.mergewise.dto.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Per-file PR change with old/new line detail, unified patch, and linked review findings.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileChangeReview {

    private String file;

    private String previousFilename;

    /** added, modified, removed, renamed */
    private String status;

    private Integer additions;

    private Integer deletions;

    private Integer changes;

    private String language;

    private String module;

    private Boolean backendCritical;

    private Integer riskScore;

    /** Unified diff from VCS (GitHub/GitLab). */
    private String patch;

    /** Lines removed in this PR (old code). */
    @Builder.Default
    private List<String> removedLines = new ArrayList<>();

    /** Lines added in this PR (new code). */
    @Builder.Default
    private List<String> addedLines = new ArrayList<>();

    /** Structured diff for UI: before/after with line numbers. */
    @Builder.Default
    private List<DiffLine> diffLines = new ArrayList<>();

    private int fileScore;

    private String risk;

    private String summary;

    @Builder.Default
    private List<ReviewIssueModel> issues = new ArrayList<>();

    @Builder.Default
    private List<String> issueIds = new ArrayList<>();

    @Builder.Default
    private List<SuggestionItem> suggestions = new ArrayList<>();
}
