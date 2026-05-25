package com.mergewise.agents.quality;

import com.mergewise.agents.core.Agent;
import com.mergewise.context.AgentContext;
import com.mergewise.dto.PRFileChange;
import com.mergewise.dto.ReviewIssue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class QualityAgent implements Agent {

 @Override
 public String getName() {
  return "QUALITY";
 }

 @Override
 public void execute(AgentContext context) {

  List<ReviewIssue> issues = context.getReviewIssues();
  if (issues == null) {
   issues = new ArrayList<>();
   context.setReviewIssues(issues);
  }

  for (PRFileChange file : context.getFileChanges()) {

   String patch = file.getPatch();

   if (patch == null) {
    continue;
   }

   // LARGE PATCH DETECTION

   if (patch.length() > 1500) {

    ReviewIssue qualityIssue = ReviewIssue.builder()
            .id(UUID.randomUUID().toString())
            .severity("MEDIUM")
            .category("CODE_QUALITY")
            .file(file.getFilename())
            .line(0)
            .title("Large Code Change Detected")
            .description("Large patch size may reduce maintainability and review quality")
            .productionImpact("Large unreviewed code changes increase production regression risk")
            .fixRecommendation("Split PR into smaller logical commits")
            .fixedCodeExample("Break changes into smaller feature-based pull requests")
            .confidenceScore(82)
            .build();

    issues.add(qualityIssue);
   }

   // SYSTEM.OUT DETECTION

   if (patch.contains("System.out.println")) {

    ReviewIssue loggingIssue = ReviewIssue.builder()
            .id(UUID.randomUUID().toString())
            .severity("LOW")
            .category("LOGGING")
            .file(file.getFilename())
            .line(0)
            .title("System.out.println Detected")
            .description("System.out.println should not be used in production applications")
            .productionImpact("Logs will not integrate properly with observability systems")
            .fixRecommendation("Use Slf4j logger instead")
            .fixedCodeExample("log.info(\"message\");")
            .confidenceScore(95)
            .build();

    issues.add(loggingIssue);
   }
  }

  context.setReviewIssues(issues);

  log.info("Quality Agent Completed");
 }
}