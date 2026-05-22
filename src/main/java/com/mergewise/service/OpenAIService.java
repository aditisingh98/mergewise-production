package com.mergewise.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OpenAIService {
 private final WebClient webClient;

 @Value("${openai.api-key:}")
 private String apiKey;

 @Value("${openai.model:gpt-4o-mini}")
 private String model;

 public boolean isConfigured() {
  return apiKey != null && !apiKey.isBlank() && !apiKey.contains("PASTE_YOUR_OPENAI_API_KEY_HERE");
 }

 public String analyzeNullPointerRisks(String patch) {
  if(!isConfigured()){
   throw new IllegalStateException("OpenAI API key is not configured");
  }

  OpenAIResponse response = webClient.post()
          .uri("https://api.openai.com/v1/chat/completions")
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey.trim())
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(Map.of(
                  "model", model,
                  "temperature", 0,
                  "messages", List.of(
                          Map.of(
                                  "role", "system",
                                  "content", "You are a senior Java code reviewer. Analyze pull request diffs only for realistic NullPointerException risks. Be concise and avoid false positives."
                          ),
                          Map.of(
                                  "role", "user",
                                  "content", buildPrompt(patch)
                          )
                  )
          ))
          .retrieve()
          .bodyToMono(OpenAIResponse.class)
          .block();

  if(response == null || response.getChoices() == null || response.getChoices().isEmpty()
          || response.getChoices().get(0).getMessage() == null){
   return "OpenAI returned no NPE analysis.";
  }

  return response.getChoices().get(0).getMessage().getContent();
 }

 private String buildPrompt(String patch) {
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
