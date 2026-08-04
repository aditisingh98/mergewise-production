package com.mergewise.dto.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ReviewIssueModel {

    private String id;

    private String fingerprint;

    private String severity;

    private String severityColor;

    private String category;

    private String explorerCategory;

    private String file;

    private Integer line;

    private String title;

    private String description;

    private String rootCause;

    private String productionImpact;

    private String recommendation;

    private String fixedExample;

    /** Line(s) in the PR that exhibit the problem (usually new-side code). */
    private String affectedCode;

    /** Single-line or short excerpt of pre-change code at the finding location. */
    private String oldCode;

    /** Single-line or short excerpt of post-change code at the finding location. */
    private String newCode;

    /** Nearby lines for UI context (includes changed and unchanged lines). */
    private String codeSnippet;

    /** Actionable guidance for developers (fix steps, patterns, conventions). */
    private String developmentGuidance;

    private CodeComparison codeComparison;

    private Integer confidence;

    private String estimatedFixTime;

    private Boolean autoFix;

    private Boolean blocking;

    @Builder.Default
    private List<String> references = new ArrayList<>();
}
