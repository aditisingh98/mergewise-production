package com.mergewise.dto.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Simplified Issues tab: every finding with issue/fix code, plus file-level added/removed lines.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssuesTabSection {

    @Builder.Default
    private List<IssueTabItem> items = new ArrayList<>();

    @Builder.Default
    private List<FileDiffSummary> fileDiffs = new ArrayList<>();
}
