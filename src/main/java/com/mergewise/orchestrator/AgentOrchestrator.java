package com.mergewise.orchestrator;

import com.mergewise.agents.npe.NpeAgent;
import com.mergewise.agents.planner.PlannerAgent;
import com.mergewise.agents.quality.QualityAgent;
import com.mergewise.context.AgentContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentOrchestrator {

 private final PlannerAgent plannerAgent;

 private final NpeAgent npeAgent;

 private final QualityAgent qualityAgent;

 public AgentContext run(AgentContext context) {

  log.info("Starting Agent Orchestration");

  // STEP 1 → PLANNER

  plannerAgent.execute(context);

  // STEP 2 → NPE ANALYSIS

  npeAgent.analyze(context);

  // STEP 3 → QUALITY ANALYSIS

  qualityAgent.execute(context);

  // STEP 4 → FINAL DECISION

  if (context.getReviewIssues().isEmpty()) {

   context.setFinalDecision("APPROVE");

  } else {

   boolean criticalIssuePresent =
           context.getReviewIssues()
                   .stream()
                   .anyMatch(issue ->
                           "CRITICAL".equalsIgnoreCase(
                                   issue.getSeverity()
                           )
                   );

   if (criticalIssuePresent) {
    context.setFinalDecision("BLOCK_MERGE");
   } else {
    context.setFinalDecision("CHANGES_REQUIRED");
   }
  }

  context.setComplete(true);

  log.info(
          "Agent Orchestration Completed. Total Issues={}",
          context.getReviewIssues().size()
  );

  return context;
 }
}