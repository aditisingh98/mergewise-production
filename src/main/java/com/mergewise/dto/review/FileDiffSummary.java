package com.mergewise.dto.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Per-file code change: only removed and added lines (no patch/diff-line parsing).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileDiffSummary {

    private String file;

    private String status;

    @Builder.Default
    private List<String> removedLines = new ArrayList<>();

    @Builder.Default
    private List<String> addedLines = new ArrayList<>();

    /** IDs of findings in {@link IssuesTabSection#getItems()} for this file. */
    @Builder.Default
    private List<String> issueIds = new ArrayList<>();
}
