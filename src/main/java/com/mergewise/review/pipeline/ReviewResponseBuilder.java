package com.mergewise.review.pipeline;

import com.mergewise.context.AgentContext;
import com.mergewise.dto.ArchitectureRecommendation;
import com.mergewise.dto.PRAnalysisResponse;
import com.mergewise.dto.PRFileChange;
import com.mergewise.dto.TestingRecommendation;
import com.mergewise.dto.review.*;
import com.mergewise.review.normalize.SeverityUtils;
import com.mergewise.service.DeploymentInfoService;
import com.mergewise.service.MergeDecisionEngine;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReviewResponseBuilder {

    private final IssueDeduplicator issueDeduplicator;
    private final IssueCategorizer issueCategorizer;
    private final ReviewScoreCalculator scoreCalculator;
    private final MergeDecisionEngine mergeDecisionEngine;
    private final DashboardBuilder dashboardBuilder;
    private final SystemStatusBuilder systemStatusBuilder;
    private final DeploymentInfoService deploymentInfoService;

    public ReviewResponseBuilder(
            IssueDeduplicator issueDeduplicator,
            IssueCategorizer issueCategorizer,
            ReviewScoreCalculator scoreCalculator,
            MergeDecisionEngine mergeDecisionEngine,
            DashboardBuilder dashboardBuilder,
            SystemStatusBuilder systemStatusBuilder,
            DeploymentInfoService deploymentInfoService) {
        this.issueDeduplicator = issueDeduplicator;
        this.issueCategorizer = issueCategorizer;
        this.scoreCalculator = scoreCalculator;
        this.mergeDecisionEngine = mergeDecisionEngine;
        this.dashboardBuilder = dashboardBuilder;
        this.systemStatusBuilder = systemStatusBuilder;
        this.deploymentInfoService = deploymentInfoService;
    }

    public PRAnalysisResponse build(AgentContext context, ReviewSummaries summaries) {
        IssueNormalizer.NormalizedIssues normalized = issueDeduplicator.deduplicate(context.getReviewIssues());
        List<ReviewIssueModel> issues = normalized.getIssues();

        ExplainedScore mergeConfidence = scoreCalculator.mergeConfidence(issues, context);
        MergeDecisionEngine.MergeDecision rawDecision = mergeDecisionEngine.decide(issues);
        MergeDecisionDetail mergeDecision = toMergeDecision(rawDecision, issues);

        ScoresSection scores = ScoresSection.builder()
                .overall(scoreCalculator.overall(issues))
                .quality(scoreCalculator.quality(issues))
                .security(scoreCalculator.security(issues))
                .performance(scoreCalculator.performance(issues))
                .maintainability(scoreCalculator.maintainability(issues))
                .complexity(scoreCalculator.complexity(context))
                .mergeConfidence(mergeConfidence)
                .build();

        return PRAnalysisResponse.builder()
                .repo(context.getRepo())
                .prNumber(context.getPrNumber())
                .deploymentInfo(deploymentInfoService.build())
                .dashboard(dashboardBuilder.build(context, issues, rawDecision, mergeConfidence))
                .mergeDecision(mergeDecision)
                .fixFirst(buildFixFirst(issues))
                .issueExplorer(issueCategorizer.categorize(issues))
                .scores(scores)
                .files(buildFiles(context, issues))
                .issues(issues)
                .testing(buildTesting(context, issues))
                .architecture(buildArchitecture(context, issues))
                .metrics(buildMetrics(context, normalized))
                .summaries(summaries)
                .systemStatus(systemStatusBuilder.build(context))
                .build();
    }

    private FixFirstSection buildFixFirst(List<ReviewIssueModel> issues) {
        List<String> topIds = issues.stream()
                .sorted(Comparator
                        .comparingInt((ReviewIssueModel i) -> SeverityUtils.rank(i.getSeverity())).reversed()
                        .thenComparing(i -> i.getConfidence() != null ? i.getConfidence() : 0, Comparator.reverseOrder())
                        .thenComparing(i -> i.getProductionImpact() != null ? i.getProductionImpact().length() : 0,
                                Comparator.reverseOrder()))
                .limit(3)
                .map(ReviewIssueModel::getId)
                .toList();

        return FixFirstSection.builder().topIssueIds(topIds).build();
    }

    private List<FileReviewRef> buildFiles(AgentContext context, List<ReviewIssueModel> issues) {
        if (context.getFileChanges() == null) {
            return List.of();
        }

        Map<String, List<ReviewIssueModel>> byFile = issues.stream()
                .filter(i -> i.getFile() != null)
                .collect(Collectors.groupingBy(ReviewIssueModel::getFile, LinkedHashMap::new, Collectors.toList()));

        List<FileReviewRef> files = new ArrayList<>();
        for (PRFileChange change : context.getFileChanges()) {
            List<ReviewIssueModel> fileIssues = byFile.getOrDefault(change.getFilename(), List.of());
            int score = fileScore(fileIssues, change);
            files.add(FileReviewRef.builder()
                    .file(change.getFilename())
                    .score(score)
                    .risk(fileRisk(score))
                    .summary(fileIssues.isEmpty()
                            ? "No issues detected."
                            : fileIssues.size() + " finding(s), " + change.getChanges() + " lines changed.")
                    .issueIds(fileIssues.stream().map(ReviewIssueModel::getId).toList())
                    .build());
        }
        return files;
    }

    private TestingView buildTesting(AgentContext context, List<ReviewIssueModel> issues) {
        List<String> testIssueIds = issues.stream()
                .filter(i -> "TESTING".equals(i.getExplorerCategory()))
                .map(ReviewIssueModel::getId)
                .toList();

        List<TestingRecommendation> recs = extractTesting(context);
        return TestingView.builder()
                .summary(testIssueIds.isEmpty()
                        ? "Test coverage appears adequate for changed files."
                        : testIssueIds.size() + " testing gap(s) identified.")
                .coverageEstimate(estimateCoverage(context, testIssueIds.size()))
                .issueIds(testIssueIds)
                .recommendationIds(recs.stream().map(r -> UUID.nameUUIDFromBytes(
                        (r.getFile() + r.getScenario()).getBytes()).toString()).toList())
                .build();
    }

    private ArchitectureView buildArchitecture(AgentContext context, List<ReviewIssueModel> issues) {
        List<String> archIds = issues.stream()
                .filter(i -> "ARCHITECTURE".equals(i.getExplorerCategory()))
                .map(ReviewIssueModel::getId)
                .toList();

        List<ArchitectureRecommendation> recs = extractArchitecture(context);
        return ArchitectureView.builder()
                .summary(archIds.isEmpty() ? "No architecture concerns detected." : archIds.size() + " architecture finding(s).")
                .issueIds(archIds)
                .recommendationIds(recs.stream().map(r -> UUID.nameUUIDFromBytes(
                        (r.getFile() + r.getPattern()).getBytes()).toString()).toList())
                .build();
    }

    private MetricsSection buildMetrics(AgentContext context, IssueNormalizer.NormalizedIssues normalized) {
        int added = 0;
        int removed = 0;
        if (context.getFileChanges() != null) {
            for (PRFileChange f : context.getFileChanges()) {
                added += f.getAdditions() != null ? f.getAdditions() : 0;
                removed += f.getDeletions() != null ? f.getDeletions() : 0;
            }
        }
        return MetricsSection.builder()
                .totalFindings(normalized.getIssues().size())
                .deduplicatedFrom(normalized.getRawCount())
                .filesAnalyzed(context.getFileChanges() != null ? context.getFileChanges().size() : 0)
                .linesAdded(added)
                .linesRemoved(removed)
                .linesChanged(added + removed)
                .analyzersRun(12)
                .analysisDurationMs(0L)
                .build();
    }

    private MergeDecisionDetail toMergeDecision(MergeDecisionEngine.MergeDecision raw, List<ReviewIssueModel> issues) {
        List<String> blockingIds = issues.stream()
                .filter(i -> Boolean.TRUE.equals(i.getBlocking()))
                .map(ReviewIssueModel::getId)
                .toList();

        List<String> nextSteps = new ArrayList<>();
        if (raw.getCriticalCount() > 0) {
            nextSteps.add("Fix all critical issues before merge.");
        }
        if (raw.getHighCount() > 0) {
            nextSteps.add("Resolve high-severity findings and add regression tests.");
        }
        if (nextSteps.isEmpty()) {
            nextSteps.add("Proceed with standard code review and CI checks.");
        }

        int confidence = switch (raw.getDecision()) {
            case "APPROVE" -> 95;
            case "APPROVE_WITH_WARNINGS" -> 75;
            case "NEEDS_CHANGES" -> 45;
            case "BLOCK_MERGE" -> 15;
            default -> 50;
        };

        return MergeDecisionDetail.builder()
                .decision(raw.getDecision())
                .reason(raw.getReasoning())
                .confidence(confidence)
                .estimatedTimeToMerge(estimateFixTime(issues))
                .blockingIssueIds(blockingIds)
                .nextSteps(nextSteps)
                .build();
    }

    private int fileScore(List<ReviewIssueModel> issues, PRFileChange change) {
        int score = 100;
        for (ReviewIssueModel issue : issues) {
            score -= SeverityUtils.penalty(issue.getSeverity());
        }
        if (Boolean.TRUE.equals(change.getBackendCritical())) {
            score -= 5;
        }
        return Math.max(0, Math.min(100, score));
    }

    private String fileRisk(int score) {
        if (score >= 80) return "LOW";
        if (score >= 60) return "MEDIUM";
        if (score >= 40) return "HIGH";
        return "CRITICAL";
    }

    private String estimateCoverage(AgentContext context, int testingIssues) {
        int files = context.getFileChanges() != null ? context.getFileChanges().size() : 0;
        long testFiles = context.getFileChanges() != null
                ? context.getFileChanges().stream().filter(f -> f.getFilename() != null && f.getFilename().contains("Test")).count()
                : 0;
        int pct = files == 0 ? 0 : (int) Math.min(100, (testFiles * 100) / Math.max(1, files));
        if (testingIssues > 0) {
            pct = Math.max(0, pct - 20);
        }
        return pct + "% estimated";
    }

    private String estimateFixTime(List<ReviewIssueModel> issues) {
        int minutes = issues.stream().mapToInt(i -> switch (SeverityUtils.normalize(i.getSeverity())) {
            case "CRITICAL" -> 240;
            case "HIGH" -> 120;
            case "MEDIUM" -> 60;
            case "LOW" -> 30;
            default -> 15;
        }).sum();
        return minutes < 60 ? minutes + " minutes" : (minutes / 60) + "h " + (minutes % 60) + "m";
    }

    @SuppressWarnings("unchecked")
    private List<ArchitectureRecommendation> extractArchitecture(AgentContext context) {
        Object raw = context.getMetadata().get("architectureRecommendations");
        return raw instanceof List<?> list ? (List<ArchitectureRecommendation>) list : List.of();
    }

    @SuppressWarnings("unchecked")
    private List<TestingRecommendation> extractTesting(AgentContext context) {
        Object raw = context.getMetadata().get("testingRecommendations");
        return raw instanceof List<?> list ? (List<TestingRecommendation>) list : List.of();
    }
}
