package com.mergewise.dto.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSection {

    private String overallStatus;

    private String riskLevel;

    private int mergeConfidence;

    private int filesChanged;

    private int criticalCount;

    private int highCount;

    private int mediumCount;

    private int lowCount;

    private int infoCount;

    private int totalFindings;

    private int totalSuggestions;

    private String estimatedFixTime;
}
