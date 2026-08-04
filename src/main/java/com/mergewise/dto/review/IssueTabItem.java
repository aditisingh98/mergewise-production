package com.mergewise.dto.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One finding for the Issues tab: problem code in the PR and the code-level fix.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueTabItem {

    private String id;

    private String severity;

    private String title;

    /** Plain-language explanation of the problem. */
    private String description;

    private String file;

    private Integer line;

    /**
     * Code in this PR that has the problem (usually a line from {@code addedLines}).
     */
    private String issueCode;

    /**
     * Suggested replacement or corrected code snippet for this issue.
     */
    private String fixCode;

    /** Text guidance when a full code replacement is not available. */
    private String fixGuide;
}
