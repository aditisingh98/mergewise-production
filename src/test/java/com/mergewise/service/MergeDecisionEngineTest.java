package com.mergewise.service;

import com.mergewise.context.AgentContext;
import com.mergewise.dto.ReviewIssue;
import com.mergewise.review.pipeline.IssueDeduplicator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MergeDecisionEngineTest {

    private final MergeDecisionEngine engine = new MergeDecisionEngine(
            new com.mergewise.review.pipeline.IssueDeduplicator(
                    new com.mergewise.review.pipeline.IssueNormalizer(
                            new com.mergewise.review.normalize.FindingNormalizer())));

    @Test
    void approvesWhenNoIssues() {
        AgentContext context = new AgentContext();
        context.setReviewIssues(List.of());

        MergeDecisionEngine.MergeDecision decision = engine.decide(context);

        assertEquals("APPROVE", decision.getDecision());
    }

    @Test
    void blocksOnCriticalIssue() {
        AgentContext context = new AgentContext();
        context.setReviewIssues(List.of(ReviewIssue.builder()
                .severity("CRITICAL")
                .title("SQL injection risk")
                .category("SECURITY")
                .build()));

        MergeDecisionEngine.MergeDecision decision = engine.decide(context);

        assertEquals("BLOCK_MERGE", decision.getDecision());
    }

    @Test
    void infrastructureIssuesDoNotAffectDecision() {
        AgentContext context = new AgentContext();
        context.setReviewIssues(List.of(ReviewIssue.builder()
                .severity("MEDIUM")
                .category("AI_REVIEW")
                .file("AI provider")
                .title("429 Too Many Requests")
                .description("gemini returned 429")
                .build()));

        MergeDecisionEngine.MergeDecision decision = engine.decide(context);

        assertEquals("APPROVE", decision.getDecision());
    }

    @Test
    void needsChangesOnHighIssue() {
        AgentContext context = new AgentContext();
        context.setReviewIssues(List.of(ReviewIssue.builder()
                .severity("HIGH")
                .title("Null pointer risk")
                .category("NPE")
                .build()));

        MergeDecisionEngine.MergeDecision decision = engine.decide(context);

        assertEquals("NEEDS_CHANGES", decision.getDecision());
        assertTrue(decision.getBlockingIssueIds() != null);
    }
}
