package com.mergewise.dto.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Side-by-side view of code affected by a finding (for PR review UI).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeComparison {

    /** Code removed or present before this change (old side). */
    @Builder.Default
    private List<String> before = new ArrayList<>();

    /** Code added or present after this change (new side). */
    @Builder.Default
    private List<String> after = new ArrayList<>();

    /** Unified snippet around the change (context + changed lines). */
    private String contextSnippet;
}
