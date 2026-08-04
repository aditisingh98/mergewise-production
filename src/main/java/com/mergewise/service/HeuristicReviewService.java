package com.mergewise.service;

import com.mergewise.dto.PRFileChange;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class HeuristicReviewService {

    private static final Pattern HUNK = Pattern.compile(
            "^@@\\s+-([0-9]+)(?:,([0-9]+))?\\s+\\+([0-9]+)(?:,([0-9]+))?\\s+@@");
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
        int oldLine = 0;
        int newLine = 0;

        for (int i = 0; i < lines.length; i++) {
            String raw = lines[i];
            if (raw.startsWith("@@")) {
                Matcher m = HUNK.matcher(raw);
                if (m.find()) {
                    oldLine = Integer.parseInt(m.group(1));
                    newLine = Integer.parseInt(m.group(3));
                }
                continue;
            }
            if (raw.startsWith("---") || raw.startsWith("+++")) {
                continue;
            }

            if (raw.startsWith("-")) {
                oldLine++;
                continue;
            }

            if (!raw.startsWith("+")) {
                if (raw.startsWith(" ")) {
                    oldLine++;
                    newLine++;
                }
                continue;
            }

            String code = raw.substring(1).trim();
            newLine++;
            if (code.isBlank() || code.startsWith("//") || code.startsWith("*")) {
                continue;
            }

            checkHardcodedSecret(filename, newLine, code, review);
            checkSystemOut(filename, newLine, code, review);
            checkTodo(filename, newLine, code, review);
            checkEmptyCatch(filename, newLine, code, review);
            checkSqlConcat(filename, newLine, code, review);
            checkWildcardImport(filename, newLine, code, review);
            checkAssignNull(filename, newLine, code, review);
            checkRiskyDereference(filename, lines, i, newLine, code, review);
        }
    }

    private void addFinding(
            Review review,
            String severity,
            String filename,
            int line,
            String code,
            String message,
            String suggestion) {
        review.findings.add(HeuristicFinding.builder()
                .severity(severity)
                .file(filename)
                .line(line)
                .affectedCode(code)
                .message(message)
                .suggestion(suggestion)
                .build());
    }

    private void checkHardcodedSecret(String filename, int line, String code, Review review) {
        if (HARDCODED_SECRET_PATTERN.matcher(code).find()) {
            addFinding(review, "HIGH", filename, line, code,
                    "Possible hardcoded credential or secret in source.",
                    "Load secrets from environment variables or a secrets manager instead of hardcoding them.");
        }
    }

    private void checkSystemOut(String filename, int line, String code, Review review) {
        if (SYSTEM_OUT_PATTERN.matcher(code).find()) {
            addFinding(review, "LOW", filename, line, code,
                    "Uses System.out/System.err instead of a structured logger.",
                    "Use SLF4J (log.info / log.error) instead of System.out.");
        }
    }

    private void checkTodo(String filename, int line, String code, Review review) {
        if (TODO_PATTERN.matcher(code).find()) {
            addFinding(review, "LOW", filename, line, code,
                    "TODO/FIXME comment may indicate incomplete work.",
                    "Track the item in an issue tracker or resolve it before merging.");
        }
    }

    private void checkEmptyCatch(String filename, int line, String code, Review review) {
        if (EMPTY_CATCH_PATTERN.matcher(code).find()) {
            addFinding(review, "MEDIUM", filename, line, code,
                    "Empty catch block silently swallows exceptions.",
                    "Log the exception with context or rethrow; do not ignore caught exceptions.");
        }
    }

    private void checkSqlConcat(String filename, int line, String code, Review review) {
        if (STRING_CONCAT_SQL_PATTERN.matcher(code).find()) {
            addFinding(review, "HIGH", filename, line, code,
                    "SQL appears built via string concatenation, risking SQL injection.",
                    "Use parameterized queries (PreparedStatement, JdbcTemplate, or JPA bindings).");
        }
    }

    private void checkWildcardImport(String filename, int line, String code, Review review) {
        if (WILDCARD_IMPORT_PATTERN.matcher(code).find()) {
            addFinding(review, "LOW", filename, line, code,
                    "Wildcard import hides which types are used.",
                    "Replace wildcard imports with explicit imports for used classes.");
        }
    }

    private void checkAssignNull(String filename, int line, String code, Review review) {
        if (code.matches(".*\\b\\w+\\s*=\\s*null\\s*;.*")) {
            addFinding(review, "MEDIUM", filename, line, code,
                    "Variable explicitly assigned null; may cause NullPointerException downstream.",
                    "Use Optional, a safe default, or validate before dereferencing.");
        }
    }

    private void checkRiskyDereference(String filename, String[] lines, int index, int line, String code, Review review) {
        Matcher matcher = DEREFERENCE_PATTERN.matcher(code);
        while (matcher.find()) {
            String value = matcher.group(1);
            if (isIgnoredDereference(code, value)) {
                continue;
            }
            if (hasNearbyNullCheck(lines, index, value)) {
                return;
            }
            addFinding(review, "MEDIUM", filename, line, code,
                    "Dereferences `" + value + "` without a visible null check.",
                    "Add `" + value + " != null` guard, Optional, or document non-null contract.");
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
        private final List<HeuristicFinding> findings = new ArrayList<>();

        /** @deprecated use {@link #getFindings()} */
        @Deprecated
        public List<String> getIssues() {
            return findings.stream()
                    .map(f -> "[" + f.getSeverity().toLowerCase() + "] " + f.getFile() + ":" + f.getLine()
                            + " - " + f.getMessage())
                    .toList();
        }

        /** @deprecated use {@link #getFindings()} */
        @Deprecated
        public List<String> getSuggestions() {
            return findings.stream()
                    .map(f -> f.getFile() + " - " + f.getSuggestion())
                    .toList();
        }

        public List<HeuristicFinding> getFindings() {
            return findings;
        }
    }

    @lombok.Data
    @lombok.Builder
    public static class HeuristicFinding {
        private String severity;
        private String file;
        private int line;
        private String affectedCode;
        private String message;
        private String suggestion;
    }
}
