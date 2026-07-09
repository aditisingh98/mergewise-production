package com.mergewise.service;

public final class AiReviewSupport {

    private AiReviewSupport() {
    }

    public static boolean isInfrastructureMessage(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String lower = text.toLowerCase();
        return lower.contains("ai provider")
                || lower.contains("429")
                || lower.contains("too many requests")
                || lower.contains("401 unauthorized")
                || lower.contains("openai_api_key")
                || lower.contains("rate limit")
                || lower.contains("quota")
                || lower.contains("gemini-")
                || (lower.startsWith("issue:") && lower.contains("ai api request failed"));
    }

    public static boolean isInfrastructureIssue(String category, String file, String title, String description) {
        if ("AI_REVIEW".equalsIgnoreCase(category)) {
            if (file != null && file.toLowerCase().contains("ai provider")) {
                return true;
            }
            if (isInfrastructureMessage(title) || isInfrastructureMessage(description)) {
                return true;
            }
        }
        return isInfrastructureMessage(title) || isInfrastructureMessage(description);
    }
}
