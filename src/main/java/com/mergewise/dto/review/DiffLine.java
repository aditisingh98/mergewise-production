package com.mergewise.dto.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiffLine {

    /** ADDED, REMOVED, CONTEXT, or HUNK */
    private String type;

    private String content;

    private Integer oldLineNumber;

    private Integer newLineNumber;
}
