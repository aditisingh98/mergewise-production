package com.mergewise.dto.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

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

    /** Finding IDs tied to this diff row (for inline annotations). */
    @Builder.Default
    private List<String> issueIds = new ArrayList<>();

    /** True when one or more findings reference this line. */
    private Boolean highlighted;
}
