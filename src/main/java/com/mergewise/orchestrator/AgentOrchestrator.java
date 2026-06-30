package com.mergewise.orchestrator;

import com.mergewise.agents.npe.NpeAgent;
import com.mergewise.agents.planner.PlannerAgent;
import com.mergewise.agents.quality.QualityAgent;
import com.mergewise.agents.review.CodeReviewAgent;
import com.mergewise.context.AgentContext;
import com.mergewise.dto.ReviewIssue;
import com.mergewise.review.AdvancedReviewEngine;
import com.mergewise.service.MergeDecisionEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentOrchestrator {

    private final PlannerAgent plannerAgent;
    private final CodeReviewAgent codeReviewAgent;
    private final NpeAgent npeAgent;
    private final QualityAgent qualityAgent;
    private final AdvancedReviewEngine advancedReviewEngine;
    private final MergeDecisionEngine mergeDecisionEngine;

    public AgentContext run(AgentContext context) {

        log.info("Starting Agent Orchestration");

        plannerAgent.execute(context);
        codeReviewAgent.execute(context);
        npeAgent.analyze(context);
        qualityAgent.execute(context);
        advancedReviewEngine.run(context);

        List<ReviewIssue> reviewIssues = context.getReviewIssues();
        if (reviewIssues == null) {
            reviewIssues = List.of();
        }

        MergeDecisionEngine.MergeDecision decision = mergeDecisionEngine.decide(context);
        context.setFinalDecision(decision.getDecision());
        context.setDecisionReasoning(decision.getReasoning());

        // Legacy alias for clients expecting CHANGES_REQUIRED
        if ("NEEDS_CHANGES".equals(decision.getDecision())) {
            context.getMetadata().put("legacyFinalDecision", "CHANGES_REQUIRED");
        }

        context.setComplete(true);

        log.info(
                "Agent Orchestration Completed. Decision={}, Total Issues={}",
                decision.getDecision(),
                reviewIssues.size());

        return context;
    }
}
