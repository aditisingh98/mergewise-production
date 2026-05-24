package com.mergewise.agents.npe;

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
public class NpeAgent {

 public void analyze(AgentContext context) {

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

   String[] lines = patch.split("\n");

   for (int i = 0; i < lines.length; i++) {

    String line = lines[i];

    // NULL assignment detection

    if (line.contains("=null")
            || line.contains("= null")) {

     ReviewIssue issue = ReviewIssue.builder()
             .id(UUID.randomUUID().toString())
             .severity("HIGH")
             .category("NPE")
             .file(file.getFilename())
             .line(i + 1)
             .title("Possible Null Assignment")
             .description("Variable assigned with null value")
             .productionImpact("May cause NullPointerException during runtime")
             .fixRecommendation("Initialize with safe default value or add null handling")
             .fixedCodeExample("String value = \"\";")
             .confidenceScore(90)
             .build();

     issues.add(issue);
    }

    // Unsafe equalsIgnoreCase

    if (line.contains(".equalsIgnoreCase(")
            && !line.contains("!= null")) {

     ReviewIssue issue = ReviewIssue.builder()
             .id(UUID.randomUUID().toString())
             .severity("CRITICAL")
             .category("NPE")
             .file(file.getFilename())
             .line(i + 1)
             .title("Unsafe Null Dereference")
             .description("equalsIgnoreCase called without null validation")
             .productionImpact("Can crash production API with NullPointerException")
             .fixRecommendation("Add null check before equalsIgnoreCase")
             .fixedCodeExample("if(value != null && value.equalsIgnoreCase(\"test\"))")
             .confidenceScore(97)
             .build();

     issues.add(issue);
    }
   }
  }

  context.setReviewIssues(issues);
 }
}