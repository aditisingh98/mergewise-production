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

    /** Simplified Issues tab: {@code items} + per-file {@code fileDiffs} (added/removed lines, issue IDs only). */
    private IssuesTabSection issuesTab;

    /** Suggestions tab: {@code issueCode} + {@code fixCode} per finding (no duplicate affected/new fields). */
    private SuggestionsTabSection suggestionsTab;

    private ScoresSection scores;

    /** Lightweight index for explorer / merge decision (full code lives under {@link #issuesTab}). */
    @Builder.Default
    private List<IssueRef> issues = new ArrayList<>();

    /**
     * @deprecated Use {@link #issuesTab} and {@link #suggestionsTab}. Kept empty to avoid duplicate payloads.
     */
    @Deprecated
    @Builder.Default
    private List<ReviewIssueModel> issueDetails = new ArrayList<>();

    @Builder.Default
    private List<FileReviewRef> files = new ArrayList<>();

    /** Per-file diff summary (patch + line lists). Nested issue payloads omitted — use {@link #issuesTab}. */
    @Builder.Default
    private List<FileChangeReview> fileChangeReviews = new ArrayList<>();

    private TestingView testing;

    private ArchitectureView architecture;

    private MetricsSection metrics;

    private ReviewSummaries summaries;

    private SystemStatus systemStatus;
}
