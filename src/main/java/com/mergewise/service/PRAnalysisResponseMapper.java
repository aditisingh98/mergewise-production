package com.mergewise.service;

import com.mergewise.context.AgentContext;
import com.mergewise.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PRAnalysisResponseMapper {

    private final ScoreCalculatorService scoreCalculator;
    private final DeploymentInfoService deploymentInfoService;

    public PRAnalysisResponse fromContext(AgentContext context, ExecutiveSummary executiveSummary) {
        List<ReviewIssue> issues = context.getReviewIssues() != null
                ? context.getReviewIssues()
                : new ArrayList<>();

        return PRAnalysisResponse.builder()
                .repo(context.getRepo())
                .prNumber(context.getPrNumber())
                .fileChanges(context.getFileChanges())
                .reviewIssues(issues)
                .metadata(context.getMetadata())
                .complete(context.isComplete())
                .overallScore(context.getOverallScore())
                .riskLevel(context.getRiskLevel())
                .finalDecision(context.getFinalDecision())
                .decisionReasoning(context.getDecisionReasoning())
                .executiveSummary(executiveSummary)
                .qualityScore(scoreCalculator.qualityScore(issues))
                .securityScore(scoreCalculator.securityScore(issues))
                .performanceScore(scoreCalculator.performanceScore(issues))
                .maintainabilityScore(scoreCalculator.maintainabilityScore(issues))
                .complexityScore(scoreCalculator.complexityScore(context))
                .mergeConfidence(scoreCalculator.mergeConfidence(context, issues))
                .reviewSuggestions(mapSuggestions(issues))
                .architectureRecommendations(extractArchitecture(context))
                .testingRecommendations(extractTesting(context))
                .deploymentInfo(deploymentInfoService.build())
                .build();
    }

    private List<ReviewSuggestion> mapSuggestions(List<ReviewIssue> issues) {
        return issues.stream().map(this::toSuggestion).toList();
    }

    private ReviewSuggestion toSuggestion(ReviewIssue issue) {
        String rootCause = issue.getRootCause() != null && !issue.getRootCause().isBlank()
                ? issue.getRootCause()
                : issue.getTitle();

        return ReviewSuggestion.builder()
                .id(issue.getId())
                .category(issue.getCategory())
                .severity(issue.getSeverity())
                .file(issue.getFile())
                .line(issue.getLine())
                .title(issue.getTitle())
                .rootCause(rootCause)
                .productionImpact(issue.getProductionImpact())
                .fixRecommendation(issue.getFixRecommendation())
                .fixedCodeExample(issue.getFixedCodeExample())
                .confidenceScore(issue.getConfidenceScore())
                .build();
    }

    @SuppressWarnings("unchecked")
    private List<ArchitectureRecommendation> extractArchitecture(AgentContext context) {
        Object raw = context.getMetadata().get("architectureRecommendations");
        if (raw instanceof List<?> list) {
            return (List<ArchitectureRecommendation>) list;
        }
        return new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    private List<TestingRecommendation> extractTesting(AgentContext context) {
        Object raw = context.getMetadata().get("testingRecommendations");
        if (raw instanceof List<?> list) {
            return (List<TestingRecommendation>) list;
        }
        return new ArrayList<>();
    }
}
