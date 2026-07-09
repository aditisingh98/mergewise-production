package com.mergewise.dto.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardView {

    private boolean canMerge;

    private String overallStatus;

    private String riskLevel;

    private int mergeConfidence;

    private int filesChanged;

    private int totalIssues;

    private int totalSuggestions;

    private String estimatedFixTime;

    @Builder.Default
    private Map<String, Integer> issueCounts = new HashMap<>();
}
