package com.mergewise.review.pipeline;

import com.mergewise.dto.PRFileChange;
import com.mergewise.dto.review.AiReviewStatus;
import com.mergewise.service.AiProviderException;
import com.mergewise.service.OpenAIService;
import lombok.Builder;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProviderFallbackService {

    private final OpenAIService openAIService;

    @Value("${openai.fallback-base-url:https://api.openai.com/v1}")
    private String fallbackBaseUrl;

    @Value("${openai.fallback-model:gpt-4o-mini}")
    private String fallbackModel;

    @Value("${openai.fallback-api-key:}")
    private String fallbackApiKey;

    public ProviderFallbackService(OpenAIService openAIService) {
        this.openAIService = openAIService;
    }

    public AiReviewAttempt reviewCode(List<PRFileChange> fileChanges) {
        if (!openAIService.isConfigured()) {
            return AiReviewAttempt.builder()
                    .status(failedStatus("NOT_CONFIGURED", null, null, false,
                            "AI provider is not configured. Static analysis completed."))
                    .build();
        }

        String primaryProvider = providerName(openAIService.getBaseUrl(), openAIService.getModel());
        try {
            String analysis = openAIService.analyzeCodeReview(fileChanges);
            return AiReviewAttempt.builder()
                    .analysis(analysis)
                    .status(successStatus(primaryProvider, false))
                    .build();
        } catch (AiProviderException primaryEx) {
            if (hasFallback()) {
                try {
                    String analysis = openAIService.analyzeCodeReviewWithConfig(
                            fallbackBaseUrl, fallbackModel, fallbackApiKey, fileChanges);
                    return AiReviewAttempt.builder()
                            .analysis(analysis)
                            .status(successStatus(providerName(fallbackBaseUrl, fallbackModel), true))
                            .build();
                } catch (AiProviderException fallbackEx) {
                    return AiReviewAttempt.builder()
                            .status(failedStatus(fallbackEx, providerName(fallbackBaseUrl, fallbackModel), true))
                            .build();
                } catch (Exception fallbackEx) {
                    return AiReviewAttempt.builder()
                            .status(failedStatus("FAILED", null, providerName(fallbackBaseUrl, fallbackModel),
                                    true, fallbackEx.getMessage()))
                            .build();
                }
            }
            return AiReviewAttempt.builder()
                    .status(failedStatus(primaryEx, primaryProvider, false))
                    .build();
        } catch (Exception ex) {
            return AiReviewAttempt.builder()
                    .status(failedStatus("FAILED", null, primaryProvider, false, ex.getMessage()))
                    .build();
        }
    }

    public AiReviewAttempt summarize(
            com.mergewise.context.AgentContext context,
            String decision,
            boolean fallbackOnly) {

        if (!openAIService.isConfigured() && !hasFallback()) {
            return AiReviewAttempt.builder()
                    .status(failedStatus("NOT_CONFIGURED", null, null, false, "Summary unavailable."))
                    .build();
        }

        if (fallbackOnly && hasFallback()) {
            return trySummary(fallbackBaseUrl, fallbackModel, fallbackApiKey, context, decision, true);
        }

        if (openAIService.isConfigured()) {
            AiReviewAttempt primary = trySummary(
                    openAIService.getBaseUrl(), openAIService.getModel(), openAIService.getApiKey(),
                    context, decision, false);
            if (primary.getAnalysis() != null) {
                return primary;
            }
        }

        if (hasFallback()) {
            return trySummary(fallbackBaseUrl, fallbackModel, fallbackApiKey, context, decision, true);
        }

        return AiReviewAttempt.builder()
                .status(failedStatus("FAILED", null, null, false, "AI summary unavailable."))
                .build();
    }

    private AiReviewAttempt trySummary(
            String baseUrl,
            String model,
            String key,
            com.mergewise.context.AgentContext context,
            String decision,
            boolean fallbackUsed) {
        try {
            String analysis = openAIService.analyzeExecutiveSummaryWithConfig(
                    baseUrl, model, key, context, decision);
            return AiReviewAttempt.builder()
                    .analysis(analysis)
                    .status(successStatus(providerName(baseUrl, model), fallbackUsed))
                    .build();
        } catch (AiProviderException ex) {
            return AiReviewAttempt.builder()
                    .status(failedStatus(ex, providerName(baseUrl, model), fallbackUsed))
                    .build();
        } catch (Exception ex) {
            return AiReviewAttempt.builder()
                    .status(failedStatus("FAILED", ex.getMessage() != null && ex.getMessage().contains("429") ? 429 : null,
                            providerName(baseUrl, model), fallbackUsed, ex.getMessage()))
                    .build();
        }
    }

    private boolean hasFallback() {
        return fallbackApiKey != null && !fallbackApiKey.isBlank();
    }

    private AiReviewStatus successStatus(String provider, boolean fallbackUsed) {
        return AiReviewStatus.builder()
                .status("COMPLETED")
                .provider(provider)
                .retryable(false)
                .fallbackUsed(fallbackUsed)
                .impact("AI insights included in review.")
                .message("AI review completed successfully.")
                .build();
    }

    private AiReviewStatus failedStatus(AiProviderException ex, String provider, boolean fallbackUsed) {
        return failedStatus(ex.getStatus(), ex.getHttpStatus(), provider, fallbackUsed, ex.getMessage());
    }

    private AiReviewStatus failedStatus(
            String errorCode,
            Integer httpStatus,
            String provider,
            boolean fallbackUsed,
            String message) {
        boolean retryable = httpStatus != null && (httpStatus == 429 || httpStatus >= 500);
        return AiReviewStatus.builder()
                .status("FAILED")
                .provider(provider)
                .httpStatus(httpStatus)
                .errorCode(errorCode)
                .retryable(retryable)
                .fallbackUsed(fallbackUsed)
                .impact("Static analysis, security, testing, runtime, architecture, and merge decision completed. AI insights unavailable.")
                .message(message)
                .build();
    }

    private String providerName(String baseUrl, String model) {
        if (baseUrl == null) {
            return model != null ? model : "unknown";
        }
        if (baseUrl.contains("generativelanguage") || baseUrl.contains("gemini")) {
            return "Gemini (" + model + ")";
        }
        if (baseUrl.contains("openai.com")) {
            return "OpenAI (" + model + ")";
        }
        return model != null ? model : baseUrl;
    }

    @Data
    @Builder
    public static class AiReviewAttempt {
        private String analysis;
        private AiReviewStatus status;
    }
}
