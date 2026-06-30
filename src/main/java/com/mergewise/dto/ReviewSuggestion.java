package com.mergewise.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewSuggestion {

    private String id;

    private String category;

    private String severity;

    private String file;

    private Integer line;

    private String title;

    private String rootCause;

    private String productionImpact;

    private String fixRecommendation;

    private String fixedCodeExample;

    private Integer confidenceScore;
}
