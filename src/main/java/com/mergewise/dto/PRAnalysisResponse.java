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

    /** Simplified Issues tab: {@code items} (issueCode + fixCode) and {@code fileDiffs} (added/removed lines). */
    private IssuesTabSection issuesTab;

    private ScoresSection scores;

    @Builder.Default
    private List<FileReviewRef> files = new ArrayList<>();

    /** Full per-file old/new diff, patch, issues, and suggestions for developer UI. */
    @Builder.Default
    private List<FileChangeReview> fileChangeReviews = new ArrayList<>();

    @Builder.Default
    private List<ReviewIssueModel> issues = new ArrayList<>();

    private TestingView testing;

    private ArchitectureView architecture;

    private MetricsSection metrics;

    private ReviewSummaries summaries;

    private SystemStatus systemStatus;
}
