package com.mergewise.review.pipeline;

import com.mergewise.context.AgentContext;
import com.mergewise.dto.ReviewIssue;
import com.mergewise.dto.review.AiReviewStatus;
import lombok.Builder;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AIReviewService {

    private final ProviderFallbackService providerFallbackService;
    private final AiReviewParser aiReviewParser;

    @Value("${mergewise.ai.enabled:true}")
    private boolean aiEnabled;

    @Value("${mergewise.ai.review-enabled:true}")
    private boolean aiReviewEnabled;

    public AIReviewService(ProviderFallbackService providerFallbackService, AiReviewParser aiReviewParser) {
        this.providerFallbackService = providerFallbackService;
        this.aiReviewParser = aiReviewParser;
    }

    public AiReviewResult review(AgentContext context) {
        if (!aiEnabled || !aiReviewEnabled) {
            return AiReviewResult.builder()
                    .issues(List.of())
                    .status(AiReviewStatus.builder()
                            .status("DISABLED")
                            .impact("Static analysis only.")
                            .message("AI review disabled by configuration.")
                            .retryable(false)
                            .fallbackUsed(false)
                            .build())
                    .build();
        }

        ProviderFallbackService.AiReviewAttempt attempt =
                providerFallbackService.reviewCode(context.getFileChanges());

        List<ReviewIssue> issues = new ArrayList<>();
        if (attempt.getAnalysis() != null) {
            issues.addAll(aiReviewParser.parse(attempt.getAnalysis()));
        }

        return AiReviewResult.builder()
                .issues(issues)
                .status(attempt.getStatus())
                .build();
    }

    @Data
    @Builder
    public static class AiReviewResult {
        private List<ReviewIssue> issues;
        private AiReviewStatus status;
    }
}
