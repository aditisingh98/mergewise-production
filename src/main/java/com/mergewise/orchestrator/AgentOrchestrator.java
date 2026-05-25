package com.mergewise.orchestrator;

import com.mergewise.agents.npe.NpeAgent;
import com.mergewise.agents.planner.PlannerAgent;
import com.mergewise.agents.quality.QualityAgent;
import com.mergewise.agents.review.CodeReviewAgent;
import com.mergewise.context.AgentContext;
import com.mergewise.dto.ReviewIssue;
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

 public AgentContext run(AgentContext context) {

  log.info("Starting Agent Orchestration");

  plannerAgent.execute(context);

  codeReviewAgent.execute(context);

  npeAgent.analyze(context);

  qualityAgent.execute(context);

  // STEP 4 → FINAL DECISION

  List<ReviewIssue> reviewIssues = context.getReviewIssues();
  if (reviewIssues == null || reviewIssues.isEmpty()) {

   context.setFinalDecision("APPROVE");

  } else {

   boolean criticalIssuePresent =
           reviewIssues
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
          reviewIssues.size()
  );

  return context;
 }
}