package com.mergewise.agents.review;

import com.mergewise.agents.core.Agent;
import com.mergewise.context.AgentContext;
import com.mergewise.dto.ReviewIssue;
import com.mergewise.service.AiProviderException;
import com.mergewise.service.AiReviewSupport;
import com.mergewise.service.HeuristicReviewService;
import com.mergewise.service.OpenAIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
@RequiredArgsConstructor
public class CodeReviewAgent implements Agent {

 private static final Pattern HEURISTIC_ISSUE_PATTERN = Pattern.compile(
         "^\\[(critical|high|medium|low)]\\s+(.+?)\\s+-\\s+(.+)$",
         Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

 private static final Pattern AI_ISSUE_PATTERN = Pattern.compile(
         "^ISSUE:\\s*\\[(critical|high|medium|low)]\\s+(.+?)\\s+-\\s+(.+)$",
         Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

 private static final Pattern AI_SUGGESTION_PATTERN = Pattern.compile(
         "^SUGGESTION:\\s*(.+)$",
         Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

 private final OpenAIService openAIService;
 private final HeuristicReviewService heuristicReviewService;

 @Value("${mergewise.ai.review-enabled:true}")
 private boolean aiReviewEnabled;

 @Override
 public String getName() {
  return "CODE_REVIEW";
 }

 @Override
 public void execute(AgentContext context) {
  ensureReviewIssues(context);
  appendHeuristicReview(context);
  appendAiReview(context);
 }

 private void ensureReviewIssues(AgentContext context) {
  if (context.getReviewIssues() == null) {
   context.setReviewIssues(new ArrayList<>());
  }
 }

 private void appendHeuristicReview(AgentContext context) {
  HeuristicReviewService.Review review = heuristicReviewService.review(context.getFileChanges());
  List<String> issueLines = review.getIssues();
  List<String> suggestionLines = review.getSuggestions();
  for (int i = 0; i < issueLines.size(); i++) {
   String suggestion = i < suggestionLines.size() ? suggestionLines.get(i) : "";
   context.getReviewIssues().add(fromHeuristicLine(issueLines.get(i), suggestion));
  }
 }

 private void appendAiReview(AgentContext context) {
  if (!aiReviewEnabled) {
   context.getMetadata().put("aiReviewEnabled", false);
   context.getMetadata().put("aiReviewStatus", "DISABLED");
   return;
  }

  if (!openAIService.isConfigured()) {
   context.getMetadata().put("aiReviewEnabled", false);
   context.getMetadata().put("aiReviewStatus", "NOT_CONFIGURED");
   return;
  }

  context.getMetadata().put("aiReviewEnabled", true);
  String analysis;
  try {
   analysis = openAIService.analyzeCodeReview(context.getFileChanges());
  } catch (AiProviderException ex) {
   log.warn("AI code review skipped ({}): {}", ex.getStatus(), ex.getMessage());
   recordAiFailure(context, ex.getStatus(), ex.getMessage());
   return;
  } catch (Exception ex) {
   log.warn("AI code review failed: {}", ex.getMessage());
   recordAiFailure(context, "FAILED", ex.getMessage());
   return;
  }

  if (AiReviewSupport.isInfrastructureMessage(analysis)) {
   log.warn("AI code review returned infrastructure message; ignoring as PR finding.");
   recordAiFailure(context, "RATE_LIMITED", analysis);
   return;
  }

  context.getMetadata().put("aiReviewStatus", "COMPLETED");
  parseAiAnalysis(analysis, context);
 }

 private void recordAiFailure(AgentContext context, String status, String message) {
  context.getMetadata().put("aiReviewStatus", status);
  context.getMetadata().put("aiReviewMessage", message);
 }

 private void parseAiAnalysis(String analysis, AgentContext context) {
  if (analysis == null || analysis.isBlank() || analysis.trim().equalsIgnoreCase("NO_ISSUES")) {
   return;
  }

  if (AiReviewSupport.isInfrastructureMessage(analysis)) {
   recordAiFailure(context, "RATE_LIMITED", analysis);
   return;
  }

  ReviewIssue pendingIssue = null;
  for (String raw : analysis.split("\\R")) {
   String line = raw.trim();
   if (line.isBlank() || AiReviewSupport.isInfrastructureMessage(line)) {
    continue;
   }

   Matcher issueMatcher = AI_ISSUE_PATTERN.matcher(line);
   if (issueMatcher.matches()) {
    if (pendingIssue != null) {
     context.getReviewIssues().add(pendingIssue);
    }
    String severity = issueMatcher.group(1).toUpperCase();
    String file = issueMatcher.group(2).trim();
    String problem = issueMatcher.group(3).trim();
    if (AiReviewSupport.isInfrastructureIssue("AI_REVIEW", file, problem, line)) {
     continue;
    }
    pendingIssue = ReviewIssue.builder()
            .id(UUID.randomUUID().toString())
            .severity(severity)
            .category("AI_REVIEW")
            .file(file)
            .line(0)
            .title(truncate(problem, 120))
            .rootCause(problem)
            .description(line)
            .productionImpact("See AI analysis in description.")
            .fixRecommendation("")
            .fixedCodeExample("")
            .confidenceScore(75)
            .build();
    continue;
   }

   if (line.regionMatches(true, 0, "ISSUE:", 0, 6)) {
    if (pendingIssue != null) {
     context.getReviewIssues().add(pendingIssue);
    }
    String body = line.substring(6).trim();
    if (AiReviewSupport.isInfrastructureMessage(body)) {
     pendingIssue = null;
     continue;
    }
    pendingIssue = ReviewIssue.builder()
            .id(UUID.randomUUID().toString())
            .severity("MEDIUM")
            .category("AI_REVIEW")
            .file("—")
            .line(0)
            .title(truncate(body, 120))
            .description(line)
            .productionImpact("See AI analysis in description.")
            .fixRecommendation("")
            .fixedCodeExample("")
            .confidenceScore(60)
            .build();
    continue;
   }

   Matcher sugMatcher = AI_SUGGESTION_PATTERN.matcher(line);
   if (sugMatcher.matches() && pendingIssue != null) {
    pendingIssue.setFixRecommendation(sugMatcher.group(1).trim());
    context.getReviewIssues().add(pendingIssue);
    pendingIssue = null;
    continue;
   }
  }
  if (pendingIssue != null) {
   if (pendingIssue.getFixRecommendation() == null || pendingIssue.getFixRecommendation().isEmpty()) {
    pendingIssue.setFixRecommendation("Apply targeted changes per the issue description.");
   }
   context.getReviewIssues().add(pendingIssue);
  }
 }

 private ReviewIssue fromHeuristicLine(String issueLine, String suggestion) {
  Matcher m = HEURISTIC_ISSUE_PATTERN.matcher(issueLine.trim());
  String severity = "MEDIUM";
  String file = "—";
  String description = issueLine;
  if (m.matches()) {
   severity = m.group(1).toUpperCase();
   file = m.group(2).trim();
   description = m.group(3).trim();
  }
  String fix = suggestion != null && !suggestion.isBlank()
          ? suggestion
          : "Address the finding using project conventions and add tests if behavior changes.";
  return ReviewIssue.builder()
          .id(UUID.randomUUID().toString())
          .severity(severity)
          .category("STATIC_ANALYSIS")
          .file(file)
          .line(0)
          .title(truncate(description, 120))
          .rootCause(description)
          .description(issueLine)
          .productionImpact("Static analysis signal; validate in context before merge.")
          .fixRecommendation(fix)
          .fixedCodeExample("")
          .confidenceScore(70)
          .build();
 }

 private static String truncate(String s, int max) {
  if (s == null) {
   return "";
  }
  return s.length() <= max ? s : s.substring(0, max) + "…";
 }
}
