package com.mergewise.service;

import com.mergewise.context.AgentContext;
import com.mergewise.dto.PRFileChange;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OpenAIService {
 private final WebClient webClient;

 @Value("${openai.api-key:}")
 private String apiKey;

 @Value("${openai.base-url:https://api.openai.com/v1}")
 private String baseUrl;

 @Value("${openai.model:gpt-4o-mini}")
 private String model;

 @Value("${openai.max-prompt-chars:12000}")
 private Integer maxPromptChars;

 public boolean isConfigured() {
  return apiKey != null && !apiKey.isBlank() && !apiKey.contains("PASTE_YOUR_OPENAI_API_KEY_HERE");
 }

 public String analyzeCodeReview(List<PRFileChange> fileChanges) {
  if(!isConfigured()){
   throw new IllegalStateException("OpenAI API key is not configured");
  }

  return chat(
          "You are a senior Java pull request reviewer. Review diffs for correctness, security, performance, maintainability, API behavior, tests, and NullPointerException risks. Return concise actionable findings only.",
          buildCodeReviewPrompt(fileChanges),
          "OpenAI returned no code review analysis."
  );
 }

 public String analyzeExecutiveSummary(AgentContext context, String decision) {
  if(!isConfigured()){
   throw new IllegalStateException("OpenAI API key is not configured");
  }

  return chat(
          "You are a principal engineer writing an executive PR review summary. Be concise, factual, and actionable in 3-5 sentences.",
          buildExecutiveSummaryPrompt(context, decision),
          "Executive summary unavailable."
  );
 }

 public String analyzeNullPointerRisks(String patch) {
  if(!isConfigured()){
   throw new IllegalStateException("OpenAI API key is not configured");
  }

  return chat(
          "You are a senior Java code reviewer. Analyze pull request diffs only for realistic NullPointerException risks. Be concise and avoid false positives.",
          buildNpePrompt(patch),
          "OpenAI returned no NPE analysis."
  );
 }

 private String chat(String systemPrompt, String userPrompt, String emptyResponseMessage) {
  OpenAIResponse response;
  try {
   response = webClient.post()
           .uri(chatCompletionsUrl())
           .headers(headers -> addAuthorizationHeader(headers, apiKey))
           .contentType(MediaType.APPLICATION_JSON)
           .bodyValue(Map.of(
                   "model", model,
                   "temperature", 0,
                   "messages", List.of(
                           Map.of(
                                   "role", "system",
                                   "content", systemPrompt
                           ),
                           Map.of(
                                   "role", "user",
                                   "content", limitPrompt(userPrompt)
                           )
                   )
           ))
           .retrieve()
           .bodyToMono(OpenAIResponse.class)
           .block();
  } catch (WebClientResponseException.TooManyRequests ex) {
   return "ISSUE: [medium] AI provider - " + model + " returned 429 Too Many Requests, so AI review could not complete."
           + "\nSUGGESTION: AI provider - Check billing/quota/rate limits, wait and retry, or configure OPENAI_BASE_URL and OPENAI_MODEL for another OpenAI-compatible provider.";
  } catch (WebClientResponseException.Unauthorized ex) {
   return "ISSUE: [high] AI provider - The configured provider rejected the API key with 401 Unauthorized."
           + "\nSUGGESTION: AI provider - Set a valid OPENAI_API_KEY for " + baseUrl + " and restart the application.";
  } catch (WebClientResponseException ex) {
   return "ISSUE: [medium] AI provider - AI API request failed with " + ex.getStatusCode()
           + "\nSUGGESTION: AI provider - Check OPENAI_BASE_URL, OPENAI_API_KEY, OPENAI_MODEL, account quota, and request size before retrying.";
  }

  if(response == null || response.getChoices() == null || response.getChoices().isEmpty()
          || response.getChoices().get(0).getMessage() == null){
   return emptyResponseMessage;
  }

  return response.getChoices().get(0).getMessage().getContent();
 }

 private String chatCompletionsUrl() {
  return baseUrl.replaceAll("/+$", "") + "/chat/completions";
 }

 private void addAuthorizationHeader(HttpHeaders headers, String token) {
  if(token != null && !token.isBlank()){
   headers.setBearerAuth(token.trim());
  }
 }

 private String limitPrompt(String prompt) {
  if(maxPromptChars == null || maxPromptChars <= 0 || prompt.length() <= maxPromptChars){
   return prompt;
  }
  return prompt.substring(0, maxPromptChars)
          + "\n\n[Diff truncated because it exceeded openai.max-prompt-chars.]";
 }

 private String buildCodeReviewPrompt(List<PRFileChange> fileChanges) {
  return """
          Review this GitHub pull request diff.

          Focus on:
          - bugs and incorrect behavior
          - NullPointerException risks
          - security issues
          - performance problems
          - maintainability/readability issues
          - missing validation
          - missing or weak tests

          Return output in this exact line format:
          ISSUE: [severity] filename - problem and impact
          SUGGESTION: filename - specific fix

          Use severity values: critical, high, medium, low.
          If no issues are found, return exactly:
          NO_ISSUES

          Pull request files:
          %s
          """.formatted(formatFileChanges(fileChanges));
 }

 private String buildExecutiveSummaryPrompt(AgentContext context, String decision) {
  int issueCount = context.getReviewIssues() != null ? context.getReviewIssues().size() : 0;
  return """
          Write an executive summary for this pull request review.

          Repository: %s
          PR Number: %s
          Risk Level: %s
          Overall Score: %s
          Total Issues: %d
          Merge Decision: %s

          Summarize business impact, key risks, and recommended next steps.
          """.formatted(
          context.getRepo(),
          context.getPrNumber(),
          context.getRiskLevel(),
          context.getOverallScore(),
          issueCount,
          decision);
 }

 private String formatFileChanges(List<PRFileChange> fileChanges) {
  StringBuilder builder = new StringBuilder();
  for(PRFileChange change : fileChanges){
   builder.append("File: ").append(change.getFilename()).append("\n");
   builder.append("Status: ").append(change.getStatus()).append("\n");
   builder.append("Additions: ").append(change.getAdditions()).append(", Deletions: ")
           .append(change.getDeletions()).append("\n");
   builder.append("Patch:\n").append(change.getPatch()).append("\n\n");
  }
  return builder.toString();
 }

 private String buildNpePrompt(String patch) {
  return """
          Review this GitHub PR patch for possible Java NullPointerException bugs.

          Return only actionable findings. For each finding include:
          - risk
          - why it can be null
          - suggested fix

          If there are no likely NPE issues, return exactly:
          No likely NPE issues found.

          Patch:
          %s
          """.formatted(patch);
 }

 @Data
 public static class OpenAIResponse {
  private List<Choice> choices;
 }

 @Data
 public static class Choice {
  private Message message;
 }

 @Data
 public static class Message {
  private String content;
 }
}
