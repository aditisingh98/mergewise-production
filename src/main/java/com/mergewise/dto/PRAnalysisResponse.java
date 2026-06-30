package com.mergewise.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PRAnalysisResponse {

    // --- Existing AgentContext fields (backward compatible) ---

    private String repo;

    private Integer prNumber;

    @Builder.Default
    private List<PRFileChange> fileChanges = new ArrayList<>();

    @Builder.Default
    private List<ReviewIssue> reviewIssues = new ArrayList<>();

    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    private boolean complete;

    private Integer overallScore;

    private String riskLevel;

    private String finalDecision;

    private String decisionReasoning;

    // --- Enterprise review enrichments ---

    private ExecutiveSummary executiveSummary;

    private ScoreBreakdown qualityScore;

    private ScoreBreakdown securityScore;

    private ScoreBreakdown performanceScore;

    private ScoreBreakdown maintainabilityScore;

    private ScoreBreakdown complexityScore;

    private ScoreBreakdown mergeConfidence;

    @Builder.Default
    private List<ReviewSuggestion> reviewSuggestions = new ArrayList<>();

    @Builder.Default
    private List<ArchitectureRecommendation> architectureRecommendations = new ArrayList<>();

    @Builder.Default
    private List<TestingRecommendation> testingRecommendations = new ArrayList<>();

    private DeploymentInfo deploymentInfo;
}
