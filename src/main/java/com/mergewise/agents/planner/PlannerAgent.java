package com.mergewise.agents.planner;
import com.mergewise.dto.PRFileChange;
import org.springframework.stereotype.Component;
import com.mergewise.agents.core.Agent;
import com.mergewise.context.AgentContext;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PlannerAgent implements Agent {

 @Override
 public String getName() {
  return "PLANNER";
 }

 @Override
 public void execute(AgentContext context) {

  log.info("Planner Agent Started");

  int totalRiskScore = 0;

  int backendCriticalFiles = 0;

  for (PRFileChange file : context.getFileChanges()) {

   if (file.getRiskScore() != null) {
    totalRiskScore += file.getRiskScore();
   }

   if (Boolean.TRUE.equals(file.getBackendCritical())) {
    backendCriticalFiles++;
   }
  }

  context.setOverallScore(
          Math.min(totalRiskScore, 100)
  );

  // RISK LEVEL

  if (totalRiskScore >= 80) {
   context.setRiskLevel("CRITICAL");
  } else if (totalRiskScore >= 60) {
   context.setRiskLevel("HIGH");
  } else if (totalRiskScore >= 30) {
   context.setRiskLevel("MEDIUM");
  } else {
   context.setRiskLevel("LOW");
  }

  // METADATA

  context.getMetadata().put(
          "backendCriticalFiles",
          backendCriticalFiles
  );

  log.info(
          "Planner completed. RiskLevel={}, Score={}",
          context.getRiskLevel(),
          context.getOverallScore()
  );
 }
}