package com.mergewise.review.normalize;

import com.mergewise.context.AgentContext;
import com.mergewise.dto.ArchitectureRecommendation;
import com.mergewise.dto.DeploymentInfo;
import com.mergewise.dto.ExecutiveSummary;
import com.mergewise.dto.PRAnalysisResponse;
import com.mergewise.dto.PRFileChange;
import com.mergewise.dto.TestingRecommendation;
import com.mergewise.dto.review.*;
import com.mergewise.service.DeploymentInfoService;
import com.mergewise.service.MergeDecisionEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewResponseAssembler {

    private final ExplainedScoreBuilder scoreBuilder;
    private final MergeDecisionEngine mergeDecisionEngine;
    private final DeploymentInfoService deploymentInfoService;

    public PRAnalysisResponse assemble(
            AgentContext context,
            FindingNormalizer.NormalizationResult normalization,
            ExecutiveSummary executiveSummary) {

        List<CanonicalFinding> findings = normalization.getFindings();
        MergeDecisionEngine.MergeDecision rawDecision = mergeDecisionEngine.decide(findings);

        ReviewSummarySection summary = buildSummary(context, executiveSummary, rawDecision);
        List<ScoreGuideEntry> scoreGuide = buildScoreGuide();
        ScoresSection scores = buildScores(context, findings);
        DashboardSection dashboard = buildDashboard(context, findings, scores, rawDecision);
        CriticalSection critical = buildCritical(findings, rawDecision);
        SecuritySection security = buildSecurity(findings);
        RuntimeSection runtime = buildRuntime(findings);
        PerformanceSection performance = buildPerformance(findings);
        MaintainabilitySection maintainability = buildMaintainability(findings);
        ArchitectureSection architecture = buildArchitecture(context, findings);
        TestingSection testing = buildTesting(context, findings);
        AiSuggestionsSection aiSuggestions = buildSuggestions(findings, architecture, testing);
        List<FileReviewItem> files = buildFileReviews(context, findings);
        MetricsSection metrics = buildMetrics(context, normalization);
        MergeDecisionDetail mergeDecision = buildMergeDecision(rawDecision, findings);

        return PRAnalysisResponse.builder()
                .repo(context.getRepo())
                .prNumber(context.getPrNumber())
                .deploymentInfo(deploymentInfoService.build())
                .summary(summary)
                .scoreGuide(scoreGuide)
                .scores(scores)
                .dashboard(dashboard)
                .critical(critical)
                .security(security)
                .runtime(runtime)
                .performance(performance)
                .maintainability(maintainability)
                .architecture(architecture)
                .testing(testing)
                .aiSuggestions(aiSuggestions)
                .files(files)
                .metrics(metrics)
                .mergeDecision(mergeDecision)
                .rawFindings(findings)
                .build();
    }

    private ReviewSummarySection buildSummary(
            AgentContext context,
            ExecutiveSummary executiveSummary,
            MergeDecisionEngine.MergeDecision decision) {

        String exec = executiveSummary != null && executiveSummary.getOverview() != null
                ? executiveSummary.getOverview()
                : "Analysis complete for " + context.getRepo() + " #" + context.getPrNumber();

        return ReviewSummarySection.builder()
                .executiveSummary(exec)
                .developerSummary(buildDeveloperSummary(context, decision))
                .reviewerSummary(buildReviewerSummary(decision))
                .releaseManagerSummary(buildReleaseManagerSummary(decision))
                .overallStatus(decision.getDecision())
                .riskLevel(context.getRiskLevel())
                .vcsProvider(stringMeta(context, "vcsProvider"))
                .repo(context.getRepo())
                .prNumber(context.getPrNumber())
                .build();
    }

    private String buildDeveloperSummary(AgentContext context, MergeDecisionEngine.MergeDecision decision) {
        return String.format(
                "You changed %d file(s). Decision: %s. Focus on blocking items first, then medium/low suggestions.",
                context.getFileChanges() != null ? context.getFileChanges().size() : 0,
                decision.getDecision());
    }

    private String buildReviewerSummary(MergeDecisionEngine.MergeDecision decision) {
        return String.format(
                "Automated review recommends %s. Critical: %d, High: %d, Medium: %d, Low: %d.",
                decision.getDecision(),
                decision.getCriticalCount(),
                decision.getHighCount(),
                decision.getMediumCount(),
                decision.getLowCount());
    }

    private String buildReleaseManagerSummary(MergeDecisionEngine.MergeDecision decision) {
        return switch (decision.getDecision()) {
            case "APPROVE" -> "Low risk for release. Standard deployment checks apply.";
            case "APPROVE_WITH_WARNINGS" -> "Acceptable with documented follow-up items post-release.";
            case "NEEDS_CHANGES" -> "Hold release until high-severity findings are resolved.";
            case "BLOCK_MERGE" -> "Do not release. Critical defects must be fixed first.";
            default -> decision.getReasoning();
        };
    }

    private List<ScoreGuideEntry> buildScoreGuide() {
        return List.of(
                entry(0, 20, "Very Dangerous", "Do not merge."),
                entry(21, 40, "High Risk", "Major fixes required."),
                entry(41, 60, "Needs Work", "Merge only after review."),
                entry(61, 80, "Good", "Minor issues remain."),
                entry(81, 90, "Very Good", "Production ready with small improvements."),
                entry(91, 100, "Excellent", "Safe to merge."));
    }

    private ScoreGuideEntry entry(int min, int max, String label, String meaning) {
        return ScoreGuideEntry.builder().min(min).max(max).label(label).meaning(meaning).build();
    }

    private ScoresSection buildScores(AgentContext context, List<CanonicalFinding> findings) {
        return ScoresSection.builder()
                .overall(scoreBuilder.buildOverallScore(findings))
                .quality(scoreBuilder.buildCategoryScore(
                        findings, Set.of("MAINTAINABILITY", "ARCHITECTURE"),
                        "Quality", "Deducts for maintainability and architecture findings."))
                .security(scoreBuilder.buildCategoryScore(
                        findings, Set.of("SECURITY"),
                        "Security", "Deducts only for normalized security findings."))
                .performance(scoreBuilder.buildCategoryScore(
                        findings, Set.of("PERFORMANCE"),
                        "Performance", "Deducts for performance anti-patterns."))
                .maintainability(scoreBuilder.buildCategoryScore(
                        findings, Set.of("MAINTAINABILITY"),
                        "Maintainability", "Deducts for code quality and readability issues."))
                .complexity(scoreBuilder.buildComplexityScore(context))
                .mergeConfidence(scoreBuilder.buildMergeConfidence(findings, context))
                .build();
    }

    private DashboardSection buildDashboard(
            AgentContext context,
            List<CanonicalFinding> findings,
            ScoresSection scores,
            MergeDecisionEngine.MergeDecision decision) {

        int critical = count(findings, "CRITICAL");
        int high = count(findings, "HIGH");
        int medium = count(findings, "MEDIUM");
        int low = count(findings, "LOW");
        int info = count(findings, "INFO");

        int suggestions = findings.stream()
                .filter(f -> !"CRITICAL".equals(f.getSeverity()))
                .mapToInt(f -> 1)
                .sum();

        return DashboardSection.builder()
                .overallStatus(decision.getDecision())
                .riskLevel(context.getRiskLevel())
                .mergeConfidence(scores.getMergeConfidence().getScore())
                .filesChanged(context.getFileChanges() != null ? context.getFileChanges().size() : 0)
                .criticalCount(critical)
                .highCount(high)
                .mediumCount(medium)
                .lowCount(low)
                .infoCount(info)
                .totalFindings(findings.size())
                .totalSuggestions(suggestions)
                .estimatedFixTime(estimateTotalFixTime(findings))
                .build();
    }

    private CriticalSection buildCritical(
            List<CanonicalFinding> findings,
            MergeDecisionEngine.MergeDecision decision) {

        List<CanonicalFinding> critical = filterSeverity(findings, "CRITICAL");
        List<CanonicalFinding> blocking = findings.stream()
                .filter(f -> Boolean.TRUE.equals(f.getBlocking()))
                .toList();
        List<CanonicalFinding> mustFix = new ArrayList<>(critical);
        for (CanonicalFinding f : blocking) {
            if (!mustFix.contains(f)) {
                mustFix.add(f);
            }
        }

        return CriticalSection.builder()
                .criticalIssues(critical)
                .blockingIssues(blocking)
                .mustFixBeforeMerge(mustFix)
                .build();
    }

    private SecuritySection buildSecurity(List<CanonicalFinding> findings) {
        List<CanonicalFinding> security = filterTab(findings, "SECURITY");
        return SecuritySection.builder()
                .securitySummary(security.isEmpty()
                        ? "No security vulnerabilities detected."
                        : security.size() + " security finding(s) require attention.")
                .issueCount(security.size())
                .securityIssues(security)
                .build();
    }

    private RuntimeSection buildRuntime(List<CanonicalFinding> findings) {
        List<CanonicalFinding> runtime = filterTab(findings, "RUNTIME");
        return RuntimeSection.builder()
                .runtimeSummary(runtime.isEmpty()
                        ? "No runtime risks detected."
                        : runtime.size() + " runtime risk(s) identified.")
                .issueCount(runtime.size())
                .runtimeIssues(runtime)
                .build();
    }

    private PerformanceSection buildPerformance(List<CanonicalFinding> findings) {
        List<CanonicalFinding> perf = filterTab(findings, "PERFORMANCE");
        return PerformanceSection.builder()
                .performanceSummary(perf.isEmpty()
                        ? "No performance issues detected."
                        : perf.size() + " performance concern(s) found.")
                .issueCount(perf.size())
                .performanceIssues(perf)
                .build();
    }

    private MaintainabilitySection buildMaintainability(List<CanonicalFinding> findings) {
        List<CanonicalFinding> items = filterTab(findings, "MAINTAINABILITY");
        return MaintainabilitySection.builder()
                .maintainabilitySummary(items.isEmpty()
                        ? "Code maintainability looks healthy."
                        : items.size() + " maintainability improvement(s) suggested.")
                .issueCount(items.size())
                .maintainabilityIssues(items)
                .build();
    }

    private ArchitectureSection buildArchitecture(AgentContext context, List<CanonicalFinding> findings) {
        List<CanonicalFinding> arch = filterTab(findings, "ARCHITECTURE");
        List<SuggestionItem> suggestions = extractArchitecture(context).stream()
                .map(this::toArchSuggestion)
                .toList();

        return ArchitectureSection.builder()
                .architectureSummary(arch.isEmpty() && suggestions.isEmpty()
                        ? "No architecture concerns detected."
                        : arch.size() + " architecture finding(s), " + suggestions.size() + " suggestion(s).")
                .issueCount(arch.size())
                .architectureIssues(arch)
                .suggestions(suggestions)
                .build();
    }

    private TestingSection buildTesting(AgentContext context, List<CanonicalFinding> findings) {
        List<CanonicalFinding> testingFindings = filterTab(findings, "TESTING");
        List<TestingRecommendation> recs = extractTesting(context);

        List<SuggestionItem> missing = new ArrayList<>();
        List<SuggestionItem> unit = new ArrayList<>();
        List<SuggestionItem> integration = new ArrayList<>();
        List<SuggestionItem> mock = new ArrayList<>();
        List<SuggestionItem> edge = new ArrayList<>();
        List<SuggestionItem> regression = new ArrayList<>();

        for (TestingRecommendation rec : recs) {
            SuggestionItem item = toTestSuggestion(rec);
            String type = rec.getTestType() != null ? rec.getTestType().toLowerCase() : "";
            if (type.contains("unit")) {
                unit.add(item);
            } else if (type.contains("integration")) {
                integration.add(item);
            } else if (type.contains("mock")) {
                mock.add(item);
            } else if (type.contains("edge")) {
                edge.add(item);
            } else if (type.contains("regression")) {
                regression.add(item);
            } else {
                missing.add(item);
            }
        }

        for (CanonicalFinding f : testingFindings) {
            missing.add(toSuggestionFromFinding(f, "MEDIUM"));
        }

        return TestingSection.builder()
                .testingSummary(testingFindings.isEmpty() && recs.isEmpty()
                        ? "Test coverage appears adequate for changed files."
                        : "Testing gaps identified. Add coverage for changed behavior.")
                .coverageEstimate(estimateCoverage(context, testingFindings))
                .missingTests(missing)
                .unitTests(unit)
                .integrationTests(integration)
                .mockTests(mock)
                .edgeCases(edge)
                .regressionTests(regression)
                .build();
    }

    private AiSuggestionsSection buildSuggestions(
            List<CanonicalFinding> findings,
            ArchitectureSection architecture,
            TestingSection testing) {

        List<SuggestionItem> quickWins = new ArrayList<>();
        List<SuggestionItem> codeQuality = new ArrayList<>();
        List<SuggestionItem> refactoring = new ArrayList<>();
        List<SuggestionItem> bestPractices = new ArrayList<>();
        List<SuggestionItem> readability = new ArrayList<>();
        List<SuggestionItem> logging = new ArrayList<>();
        List<SuggestionItem> testingItems = new ArrayList<>();
        List<SuggestionItem> architectureItems = new ArrayList<>(architecture.getSuggestions());
        List<SuggestionItem> performance = new ArrayList<>();
        List<SuggestionItem> security = new ArrayList<>();

        for (CanonicalFinding f : findings) {
            SuggestionItem item = toSuggestionFromFinding(f, effortForSeverity(f.getSeverity()));
            switch (f.getTab()) {
                case "SECURITY" -> security.add(item);
                case "RUNTIME" -> bestPractices.add(item);
                case "PERFORMANCE" -> performance.add(item);
                case "ARCHITECTURE" -> architectureItems.add(item);
                case "TESTING" -> testingItems.add(item);
                case "MAINTAINABILITY" -> {
                    if ("LOGGING".equals(f.getCategory()) || contains(f.getTitle(), "log")) {
                        logging.add(item);
                    } else if ("LOW".equals(f.getSeverity())) {
                        quickWins.add(item);
                    } else if (contains(f.getTitle(), "refactor")) {
                        refactoring.add(item);
                    } else if (contains(f.getTitle(), "read")) {
                        readability.add(item);
                    } else {
                        codeQuality.add(item);
                    }
                }
                default -> bestPractices.add(item);
            }
        }

        testingItems.addAll(flattenTesting(testing));

        return AiSuggestionsSection.builder()
                .quickWins(quickWins)
                .codeQuality(codeQuality)
                .refactoring(refactoring)
                .bestPractices(bestPractices)
                .readability(readability)
                .logging(logging)
                .testing(testingItems)
                .architecture(architectureItems)
                .performance(performance)
                .security(security)
                .build();
    }

    private List<SuggestionItem> flattenTesting(TestingSection testing) {
        List<SuggestionItem> all = new ArrayList<>();
        all.addAll(testing.getMissingTests());
        all.addAll(testing.getUnitTests());
        all.addAll(testing.getIntegrationTests());
        all.addAll(testing.getMockTests());
        all.addAll(testing.getEdgeCases());
        all.addAll(testing.getRegressionTests());
        return all;
    }

    private List<FileReviewItem> buildFileReviews(AgentContext context, List<CanonicalFinding> findings) {
        if (context.getFileChanges() == null) {
            return List.of();
        }

        Map<String, List<CanonicalFinding>> byFile = findings.stream()
                .filter(f -> f.getFile() != null)
                .collect(Collectors.groupingBy(CanonicalFinding::getFile, LinkedHashMap::new, Collectors.toList()));

        List<FileReviewItem> items = new ArrayList<>();
        for (PRFileChange change : context.getFileChanges()) {
            String file = change.getFilename();
            List<CanonicalFinding> fileFindings = byFile.getOrDefault(file, List.of());
            int score = fileScore(fileFindings, change);
            String risk = fileRisk(score);

            items.add(FileReviewItem.builder()
                    .file(file)
                    .score(score)
                    .risk(risk)
                    .summary(fileSummary(fileFindings, change))
                    .issues(fileFindings)
                    .suggestions(fileFindings.stream()
                            .filter(f -> !"CRITICAL".equals(f.getSeverity()))
                            .map(f -> toSuggestionFromFinding(f, effortForSeverity(f.getSeverity())))
                            .toList())
                    .build());
        }
        return items;
    }

    private MetricsSection buildMetrics(AgentContext context, FindingNormalizer.NormalizationResult normalization) {
        int added = 0;
        int removed = 0;
        if (context.getFileChanges() != null) {
            for (PRFileChange f : context.getFileChanges()) {
                added += f.getAdditions() != null ? f.getAdditions() : 0;
                removed += f.getDeletions() != null ? f.getDeletions() : 0;
            }
        }

        Object duration = context.getMetadata().get("analysisDurationMs");

        return MetricsSection.builder()
                .totalFindings(normalization.getDeduplicatedCount())
                .deduplicatedFrom(normalization.getRawCount())
                .filesAnalyzed(context.getFileChanges() != null ? context.getFileChanges().size() : 0)
                .linesAdded(added)
                .linesRemoved(removed)
                .linesChanged(added + removed)
                .analyzersRun(12)
                .analysisDurationMs(duration instanceof Number n ? n.longValue() : 0L)
                .build();
    }

    private MergeDecisionDetail buildMergeDecision(
            MergeDecisionEngine.MergeDecision raw,
            List<CanonicalFinding> findings) {

        List<String> blockingIds = findings.stream()
                .filter(f -> Boolean.TRUE.equals(f.getBlocking()))
                .map(CanonicalFinding::getId)
                .toList();

        List<String> nextSteps = new ArrayList<>();
        if (raw.getCriticalCount() > 0) {
            nextSteps.add("Fix all critical findings before re-running analysis.");
        }
        if (raw.getHighCount() > 0) {
            nextSteps.add("Resolve high-severity issues and add regression tests.");
        }
        if (raw.getMediumCount() > 0 || raw.getLowCount() > 0) {
            nextSteps.add("Track medium/low items as follow-up tasks if merging with warnings.");
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
                .estimatedTimeToMerge(estimateTotalFixTime(findings))
                .blockingIssueIds(blockingIds)
                .nextSteps(nextSteps)
                .build();
    }

    private SuggestionItem toSuggestionFromFinding(CanonicalFinding f, String effort) {
        return SuggestionItem.builder()
                .id(f.getId())
                .title(f.getTitle())
                .description(f.getDescription())
                .why(f.getRootCause())
                .expectedBenefit(f.getProductionImpact() != null
                        ? "Reduces: " + f.getProductionImpact()
                        : "Improves code quality and reliability.")
                .estimatedEffort(effort)
                .priority(f.getSeverity())
                .file(f.getFile())
                .line(f.getLine())
                .build();
    }

    private SuggestionItem toArchSuggestion(ArchitectureRecommendation rec) {
        return SuggestionItem.builder()
                .id(UUID.randomUUID().toString())
                .title(rec.getPattern() != null ? rec.getPattern() : "Architecture improvement")
                .description(rec.getObservation())
                .why(rec.getImpact())
                .expectedBenefit(rec.getRecommendation())
                .estimatedEffort("MEDIUM")
                .priority("MEDIUM")
                .file(rec.getFile())
                .build();
    }

    private SuggestionItem toTestSuggestion(TestingRecommendation rec) {
        return SuggestionItem.builder()
                .id(UUID.randomUUID().toString())
                .title(rec.getScenario() != null ? rec.getScenario() : "Add test coverage")
                .description(rec.getRecommendation())
                .why("Prevent regressions in changed behavior.")
                .expectedBenefit("Higher confidence during merge and release.")
                .estimatedEffort(rec.getPriority() != null ? rec.getPriority() : "MEDIUM")
                .priority(rec.getPriority())
                .file(rec.getFile())
                .build();
    }

    private int fileScore(List<CanonicalFinding> findings, PRFileChange change) {
        int score = 100;
        for (CanonicalFinding f : findings) {
            score -= SeverityUtils.penalty(f.getSeverity());
        }
        if (Boolean.TRUE.equals(change.getBackendCritical())) {
            score -= 5;
        }
        return Math.max(0, Math.min(100, score));
    }

    private String fileRisk(int score) {
        if (score >= 80) {
            return "LOW";
        }
        if (score >= 60) {
            return "MEDIUM";
        }
        if (score >= 40) {
            return "HIGH";
        }
        return "CRITICAL";
    }

    private String fileSummary(List<CanonicalFinding> findings, PRFileChange change) {
        if (findings.isEmpty()) {
            return "No issues detected in this file.";
        }
        return findings.size() + " finding(s). " + change.getChanges() + " lines changed.";
    }

    private String estimateCoverage(AgentContext context, List<CanonicalFinding> testingFindings) {
        int files = context.getFileChanges() != null ? context.getFileChanges().size() : 0;
        long testFiles = context.getFileChanges() != null
                ? context.getFileChanges().stream()
                .filter(f -> f.getFilename() != null && f.getFilename().contains("Test"))
                .count()
                : 0;
        if (files == 0) {
            return "UNKNOWN";
        }
        int pct = (int) Math.min(100, (testFiles * 100) / Math.max(1, files));
        if (!testingFindings.isEmpty()) {
            pct = Math.max(0, pct - 20);
        }
        return pct + "% estimated";
    }

    private String estimateTotalFixTime(List<CanonicalFinding> findings) {
        if (findings.isEmpty()) {
            return "0 minutes";
        }
        int minutes = findings.stream().mapToInt(f -> switch (SeverityUtils.normalize(f.getSeverity())) {
            case "CRITICAL" -> 240;
            case "HIGH" -> 120;
            case "MEDIUM" -> 60;
            case "LOW" -> 30;
            default -> 15;
        }).sum();
        if (minutes < 60) {
            return minutes + " minutes";
        }
        return (minutes / 60) + "h " + (minutes % 60) + "m";
    }

    private List<CanonicalFinding> filterTab(List<CanonicalFinding> findings, String tab) {
        return findings.stream().filter(f -> tab.equals(f.getTab())).toList();
    }

    private List<CanonicalFinding> filterSeverity(List<CanonicalFinding> findings, String severity) {
        return findings.stream().filter(f -> severity.equals(f.getSeverity())).toList();
    }

    private int count(List<CanonicalFinding> findings, String severity) {
        return (int) findings.stream().filter(f -> severity.equals(f.getSeverity())).count();
    }

    private boolean contains(String text, String needle) {
        return text != null && text.toLowerCase().contains(needle);
    }

    private String effortForSeverity(String severity) {
        return switch (SeverityUtils.normalize(severity)) {
            case "CRITICAL", "HIGH" -> "HIGH";
            case "MEDIUM" -> "MEDIUM";
            default -> "LOW";
        };
    }

    private String stringMeta(AgentContext context, String key) {
        Object value = context.getMetadata().get(key);
        return value != null ? value.toString() : null;
    }

    @SuppressWarnings("unchecked")
    private List<ArchitectureRecommendation> extractArchitecture(AgentContext context) {
        Object raw = context.getMetadata().get("architectureRecommendations");
        if (raw instanceof List<?> list) {
            return (List<ArchitectureRecommendation>) list;
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private List<TestingRecommendation> extractTesting(AgentContext context) {
        Object raw = context.getMetadata().get("testingRecommendations");
        if (raw instanceof List<?> list) {
            return (List<TestingRecommendation>) list;
        }
        return List.of();
    }
}
