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

    private DashboardView dashboard;

    private MergeDecisionDetail mergeDecision;

    private FixFirstSection fixFirst;

    private IssueExplorerSection issueExplorer;

    private ScoresSection scores;

    @Builder.Default
    private List<FileReviewRef> files = new ArrayList<>();

    @Builder.Default
    private List<ReviewIssueModel> issues = new ArrayList<>();

    private TestingView testing;

    private ArchitectureView architecture;

    private MetricsSection metrics;

    private ReviewSummaries summaries;

    private SystemStatus systemStatus;
}
