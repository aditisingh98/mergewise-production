package com.mergewise.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiReviewSupportTest {

    @Test
    void detects429InfrastructureMessage() {
        assertTrue(AiReviewSupport.isInfrastructureMessage(
                "gemini-2.0-flash returned 429 Too Many Requests, so AI review could not complete."));
    }

    @Test
    void detectsAiProviderIssueCategory() {
        assertTrue(AiReviewSupport.isInfrastructureIssue(
                "AI_REVIEW",
                "AI provider",
                "429 Too Many Requests",
                "ISSUE: [medium] AI provider - rate limited"));
    }

    @Test
    void allowsRealCodeFinding() {
        assertFalse(AiReviewSupport.isInfrastructureIssue(
                "AI_REVIEW",
                "src/Main.java",
                "Possible null dereference",
                "Variable may be null before use"));
    }
}
