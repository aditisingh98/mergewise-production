package com.mergewise.agents.review;

import com.mergewise.agents.core.Agent;
import com.mergewise.context.AgentContext;
import com.mergewise.service.HeuristicReviewService;
import com.mergewise.service.OpenAIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class CodeReviewAgent implements Agent {
 private final OpenAIService openAIService;
 private final HeuristicReviewService heuristicReviewService;

 public String getName(){return "CODE_REVIEW";}

 public void execute(AgentContext context){
  runHeuristicReview(context);
  runAiReview(context);
  context.getMetadata().put("codeReviewComplete", true);
 }

 private void runHeuristicReview(AgentContext context) {
  HeuristicReviewService.Review review = heuristicReviewService.review(context.getFileChanges());
  review.getIssues().forEach(context.getIssues()::add);
  review.getSuggestions().forEach(context.getSuggestions()::add);
 }

 private void runAiReview(AgentContext context) {
  if(!openAIService.isConfigured()){
   context.getSuggestions().add(
           "AI provider - Set OPENAI_API_KEY (and optionally OPENAI_BASE_URL/OPENAI_MODEL) to enable full AI code review.");
   return;
  }

  String analysis;
  try {
   analysis = openAIService.analyzeCodeReview(context.getFileChanges());
  } catch (Exception ex) {
   log.warn("AI code review failed, returning heuristic results only: {}", ex.getMessage());
   context.getIssues().add("[medium] AI provider - AI code review failed: " + ex.getMessage());
   context.getSuggestions().add(
           "AI provider - Check OPENAI_BASE_URL, OPENAI_API_KEY, OPENAI_MODEL, and provider quota before retrying.");
   return;
  }

  applyAnalysis(analysis, context);
 }

 private void applyAnalysis(String analysis, AgentContext context) {
  if(analysis == null || analysis.isBlank() || analysis.trim().equalsIgnoreCase("NO_ISSUES")){
   return;
  }

  for(String line : analysis.split("\\R")){
   String item = line.trim();
   if(item.isBlank()){
    continue;
   }
   if(item.startsWith("ISSUE:")){
    context.getIssues().add(item.substring("ISSUE:".length()).trim());
   } else if(item.startsWith("SUGGESTION:")){
    context.getSuggestions().add(item.substring("SUGGESTION:".length()).trim());
   } else {
    context.getSuggestions().add(item);
   }
  }
 }
}
