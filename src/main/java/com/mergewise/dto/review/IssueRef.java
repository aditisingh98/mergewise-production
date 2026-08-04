package com.mergewise.dto.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lightweight issue pointer for explorer, scores, and merge decision (no code payload).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueRef {

    private String id;

    private String severity;

    private String category;

    private String explorerCategory;

    private String title;

    private String file;

    private Integer line;

    private Boolean blocking;
}
