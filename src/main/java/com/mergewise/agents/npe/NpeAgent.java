package com.mergewise.agents.npe;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.mergewise.agents.core.Agent;
import com.mergewise.context.AgentContext;
import com.mergewise.service.OpenAIService;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class NpeAgent implements Agent{
 private static final Pattern DEREFERENCE_PATTERN = Pattern.compile("\\b([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\.");
 private static final Pattern RISKY_VALUE_PATTERN = Pattern.compile("\\b(null|findBy|orElse\\(null\\)|getBody\\(\\)|get\\([^)]*\\))\\b");
 private final OpenAIService openAIService;

 public String getName(){return "NPE";}
 public void execute(AgentContext c){
  for(String f:c.getFiles()){
   if(openAIService.isConfigured()){
    addAiAnalysis(f, c);
   } else {
    analyzePatch(f, c);
   }
  }
 }

 private void addAiAnalysis(String patch, AgentContext c) {
  String analysis = openAIService.analyzeNullPointerRisks(patch);
  if(analysis == null || analysis.isBlank() || analysis.trim().equalsIgnoreCase("No likely NPE issues found.")){
   return;
  }
  c.getIssues().add("AI NPE analysis: " + analysis.trim());
  c.getSuggestions().add("AI suggested NPE fix: " + analysis.trim());
 }

 private void analyzePatch(String patch, AgentContext c) {
  String[] lines = patch.split("\\R");

  for(int i = 0; i < lines.length; i++){
   String line = lines[i];
   if(!line.startsWith("+") || line.startsWith("+++")){
    continue;
   }

   String code = line.substring(1).trim();
   if(code.isBlank() || code.startsWith("//")){
    continue;
   }

   String dereferencedValue = findRiskyDereference(code);
   if(dereferencedValue != null && !hasNearbyNullCheck(lines, i)){
    c.getIssues().add("Possible NPE: added dereference without nearby null check -> " + code);
    c.getSuggestions().add("Add a null check before using `" + dereferencedValue + "`, for example `"
            + dereferencedValue + " != null && " + code + "` or use `Optional` if absence is expected.");
   }

   if(isRiskyNullValue(code)){
    c.getIssues().add("Possible NPE: added code may produce a nullable value -> " + code);
    c.getSuggestions().add("Avoid assigning or returning nullable values in `" + code
            + "`. Initialize it with a safe default, validate it before use, or handle the null case explicitly.");
   }
  }
 }

 private String findRiskyDereference(String code) {
  Matcher matcher = DEREFERENCE_PATTERN.matcher(code);
  while(matcher.find()){
   String value = matcher.group(1);
   if(isIgnoredDereference(code, value)){
    continue;
   }
   return value;
  }
  return null;
 }

 private boolean isIgnoredDereference(String code, String value) {
  return code.startsWith("import ")
          || code.contains("System.out.")
          || code.contains("log.")
          || Character.isUpperCase(value.charAt(0));
 }

 private boolean isRiskyNullValue(String code) {
  return RISKY_VALUE_PATTERN.matcher(code).find()
          && (code.contains("=") || code.startsWith("return ") || code.contains(".get("));
 }

 private boolean hasNearbyNullCheck(String[] lines, int currentLine) {
  int start = Math.max(0, currentLine - 4);
  for(int i = start; i <= currentLine; i++){
   String line = lines[i].trim();
   if(line.contains("!= null") || line.contains("== null") || line.contains("Objects.nonNull")
           || line.contains("Objects.isNull") || line.contains("Optional.ofNullable")){
    return true;
   }
  }
  return false;
 }
}