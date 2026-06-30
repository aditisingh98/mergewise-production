package com.mergewise.service;

import com.mergewise.context.AgentContext;
import com.mergewise.dto.ReviewIssue;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MergeDecisionEngineTest {

    private final MergeDecisionEngine engine = new MergeDecisionEngine();

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
        context.setReviewIssues(List.of(ReviewIssue.builder().severity("CRITICAL").build()));

        MergeDecisionEngine.MergeDecision decision = engine.decide(context);

        assertEquals("BLOCK_MERGE", decision.getDecision());
    }

    @Test
    void needsChangesOnHighIssue() {
        AgentContext context = new AgentContext();
        context.setReviewIssues(List.of(ReviewIssue.builder().severity("HIGH").build()));

        MergeDecisionEngine.MergeDecision decision = engine.decide(context);

        assertEquals("NEEDS_CHANGES", decision.getDecision());
    }

    @Test
    void approvesWithWarningsOnMediumOnly() {
        AgentContext context = new AgentContext();
        context.setReviewIssues(List.of(ReviewIssue.builder().severity("MEDIUM").build()));

        MergeDecisionEngine.MergeDecision decision = engine.decide(context);

        assertEquals("APPROVE_WITH_WARNINGS", decision.getDecision());
    }
}
