package com.mergewise.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutiveSummary {

    private String overview;

    private String functionalImpact;

    private String riskAssessment;

    private String recommendedAction;

    private int totalIssues;

    private int criticalIssues;

    private int highIssues;

    private int filesChanged;
}
