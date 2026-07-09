package com.mergewise.dto;

import com.mergewise.dto.review.*;
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
public class PRAnalysisResponse {

    private String repo;

    private Integer prNumber;

    private DeploymentInfo deploymentInfo;

    private ReviewSummarySection summary;

    @Builder.Default
    private List<ScoreGuideEntry> scoreGuide = new ArrayList<>();

    private ScoresSection scores;

    private DashboardSection dashboard;

    private CriticalSection critical;

    private SecuritySection security;

    private RuntimeSection runtime;

    private PerformanceSection performance;

    private MaintainabilitySection maintainability;

    private ArchitectureSection architecture;

    private TestingSection testing;

    private AiSuggestionsSection aiSuggestions;

    @Builder.Default
    private List<FileReviewItem> files = new ArrayList<>();

    private MetricsSection metrics;

    private MergeDecisionDetail mergeDecision;

    @Builder.Default
    private List<CanonicalFinding> rawFindings = new ArrayList<>();
}
