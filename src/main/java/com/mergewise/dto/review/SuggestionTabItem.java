package com.mergewise.dto.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Suggestions tab row: problematic line and corrected code (no duplicate affected/new fields).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuggestionTabItem {

    private String id;

    private String severity;

    private String title;

    private String file;

    private Integer line;

    private String issueCode;

    private String fixCode;

    private String fixGuide;
}
