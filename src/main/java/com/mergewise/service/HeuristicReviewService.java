package com.mergewise.service;

import com.mergewise.dto.PRFileChange;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class HeuristicReviewService {

 private static final Pattern DEREFERENCE_PATTERN = Pattern.compile("\\b([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\.");
 private static final Pattern HARDCODED_SECRET_PATTERN = Pattern.compile(
         "(?i)(api[_-]?key|secret|password|token)\\s*[:=]\\s*[\"'][^\"']{6,}[\"']");
 private static final Pattern SYSTEM_OUT_PATTERN = Pattern.compile("\\bSystem\\.(out|err)\\.print");
 private static final Pattern TODO_PATTERN = Pattern.compile("(?i)\\b(TODO|FIXME|XXX)\\b");
 private static final Pattern EMPTY_CATCH_PATTERN = Pattern.compile("catch\\s*\\([^)]+\\)\\s*\\{\\s*\\}");
 private static final Pattern STRING_CONCAT_SQL_PATTERN = Pattern.compile("(?i)(select|insert|update|delete)\\s.*\\+\\s*[a-zA-Z_]");
 private static final Pattern WILDCARD_IMPORT_PATTERN = Pattern.compile("^import\\s+[\\w.]+\\.\\*;");

 public Review review(List<PRFileChange> fileChanges) {
  Review review = new Review();
  if (fileChanges == null || fileChanges.isEmpty()) {
   return review;
  }

  for (PRFileChange change : fileChanges) {
   reviewFile(change, review);
  }
  return review;
 }

 private void reviewFile(PRFileChange change, Review review) {
  String patch = change.getPatch();
  if (patch == null || patch.isBlank()) {
   return;
  }

  String filename = change.getFilename() != null ? change.getFilename() : "unknown";
  String[] lines = patch.split("\\R");

  for (int i = 0; i < lines.length; i++) {
   String line = lines[i];
   if (!line.startsWith("+") || line.startsWith("+++")) {
    continue;
   }

   String code = line.substring(1).trim();
   if (code.isBlank() || code.startsWith("//") || code.startsWith("*")) {
    continue;
   }

   checkHardcodedSecret(filename, code, review);
   checkSystemOut(filename, code, review);
   checkTodo(filename, code, review);
   checkEmptyCatch(filename, code, review);
   checkSqlConcat(filename, code, review);
   checkWildcardImport(filename, code, review);
   checkAssignNull(filename, code, review);
   checkRiskyDereference(filename, lines, i, code, review);
  }
 }

 private void checkHardcodedSecret(String filename, String code, Review review) {
  if (HARDCODED_SECRET_PATTERN.matcher(code).find()) {
   review.addIssue("[high] " + filename + " - Possible hardcoded credential/secret in source.");
   review.addSuggestion(filename + " - Load secrets from environment variables or a secrets manager instead of hardcoding them.");
  }
 }

 private void checkSystemOut(String filename, String code, Review review) {
  if (SYSTEM_OUT_PATTERN.matcher(code).find()) {
   review.addIssue("[low] " + filename + " - Uses System.out/System.err instead of a logger.");
   review.addSuggestion(filename + " - Use the SLF4J logger (e.g. log.info / log.error) instead of System.out.");
  }
 }

 private void checkTodo(String filename, String code, Review review) {
  if (TODO_PATTERN.matcher(code).find()) {
   review.addIssue("[low] " + filename + " - Added TODO/FIXME comment that may indicate incomplete work.");
   review.addSuggestion(filename + " - Track the TODO in an issue tracker or resolve it before merging.");
  }
 }

 private void checkEmptyCatch(String filename, String code, Review review) {
  if (EMPTY_CATCH_PATTERN.matcher(code).find()) {
   review.addIssue("[medium] " + filename + " - Empty catch block silently swallows exceptions.");
   review.addSuggestion(filename + " - Log the exception or rethrow it; never silently ignore caught exceptions.");
  }
 }

 private void checkSqlConcat(String filename, String code, Review review) {
  if (STRING_CONCAT_SQL_PATTERN.matcher(code).find()) {
   review.addIssue("[high] " + filename + " - SQL appears to be built via string concatenation, risking SQL injection.");
   review.addSuggestion(filename + " - Use parameterized queries (PreparedStatement, JdbcTemplate parameters, or JPA bindings).");
  }
 }

 private void checkWildcardImport(String filename, String code, Review review) {
  if (WILDCARD_IMPORT_PATTERN.matcher(code).find()) {
   review.addIssue("[low] " + filename + " - Wildcard import hides which classes are used.");
   review.addSuggestion(filename + " - Replace the wildcard import with explicit imports for the classes used.");
  }
 }

 private void checkAssignNull(String filename, String code, Review review) {
  if (code.matches(".*\\b\\w+\\s*=\\s*null\\s*;.*")) {
   review.addIssue("[medium] " + filename + " - Variable explicitly assigned null, which can lead to NullPointerException downstream.");
   review.addSuggestion(filename + " - Initialize with a safe default, use Optional, or validate the value before dereferencing it.");
  }
 }

 private void checkRiskyDereference(String filename, String[] lines, int index, String code, Review review) {
  Matcher matcher = DEREFERENCE_PATTERN.matcher(code);
  while (matcher.find()) {
   String value = matcher.group(1);
   if (isIgnoredDereference(code, value)) {
    continue;
   }
   if (hasNearbyNullCheck(lines, index, value)) {
    return;
   }
   review.addIssue("[medium] " + filename + " - Dereferences `" + value + "` without a visible null check: " + code);
   review.addSuggestion(filename + " - Add `" + value + " != null` check, use Optional, or annotate the value to make its nullability explicit.");
   return;
  }
 }

 private boolean isIgnoredDereference(String code, String value) {
  if (value == null || value.isEmpty()) {
   return true;
  }
  return code.startsWith("import ")
          || code.contains("System.out.")
          || code.contains("System.err.")
          || code.contains("log.")
          || code.contains("logger.")
          || Character.isUpperCase(value.charAt(0));
 }

 private boolean hasNearbyNullCheck(String[] lines, int currentLine, String value) {
  int start = Math.max(0, currentLine - 4);
  for (int i = start; i <= currentLine; i++) {
   String line = lines[i].trim();
   if (line.contains(value + " != null") || line.contains(value + " == null")
           || line.contains("Objects.nonNull(" + value)
           || line.contains("Objects.isNull(" + value)
           || line.contains("Optional.ofNullable(" + value)) {
    return true;
   }
  }
  return false;
 }

 public static class Review {
  private final List<String> issues = new ArrayList<>();
  private final List<String> suggestions = new ArrayList<>();

  public void addIssue(String issue) { issues.add(issue); }
  public void addSuggestion(String suggestion) { suggestions.add(suggestion); }
  public List<String> getIssues() { return issues; }
  public List<String> getSuggestions() { return suggestions; }
 }
}
