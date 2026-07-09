package com.mergewise.dto.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewSummarySection {

    private String executiveSummary;

    private String developerSummary;

    private String reviewerSummary;

    private String releaseManagerSummary;

    private String overallStatus;

    private String riskLevel;

    private String vcsProvider;

    private String repo;

    private Integer prNumber;
}
