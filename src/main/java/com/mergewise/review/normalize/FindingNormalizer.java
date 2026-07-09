package com.mergewise.review.normalize;

import com.mergewise.dto.ReviewIssue;
import com.mergewise.dto.review.CanonicalFinding;
import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Component
public class FindingNormalizer {

    public NormalizationResult normalize(List<ReviewIssue> rawIssues) {
        if (rawIssues == null || rawIssues.isEmpty()) {
            return NormalizationResult.builder()
                    .findings(List.of())
                    .rawCount(0)
                    .deduplicatedCount(0)
                    .build();
        }

        Map<String, CanonicalFinding> merged = new LinkedHashMap<>();

        for (ReviewIssue issue : rawIssues) {
            if (AiReviewSupport.isInfrastructureIssue(
                    issue.getCategory(), issue.getFile(), issue.getTitle(), issue.getDescription())) {
                continue;
            }
            String tab = resolveTab(issue);
            String patternKey = detectPatternKey(issue);
            String fingerprint = fingerprint(tab, issue.getFile(), issue.getLine(), patternKey);

            CanonicalFinding candidate = toCanonical(issue, tab, fingerprint, patternKey);

            if (merged.containsKey(fingerprint)) {
                merged.put(fingerprint, merge(merged.get(fingerprint), candidate, issue.getCategory()));
            } else {
                merged.put(fingerprint, candidate);
            }
        }

        List<CanonicalFinding> findings = new ArrayList<>(merged.values());
        return NormalizationResult.builder()
                .findings(findings)
                .rawCount(rawIssues.size())
                .deduplicatedCount(findings.size())
                .build();
    }

    private CanonicalFinding merge(CanonicalFinding existing, CanonicalFinding incoming, String sourceCategory) {
        String severity = SeverityUtils.maxSeverity(existing.getSeverity(), incoming.getSeverity());
        List<String> mergedFrom = new ArrayList<>(existing.getMergedFrom());
        if (sourceCategory != null && !mergedFrom.contains(sourceCategory)) {
            mergedFrom.add(sourceCategory);
        }
        for (String src : incoming.getMergedFrom()) {
            if (!mergedFrom.contains(src)) {
                mergedFrom.add(src);
            }
        }

        return existing.toBuilder()
                .severity(severity)
                .severityColor(SeverityUtils.colorFor(severity))
                .blocking(SeverityUtils.isBlocking(severity))
                .description(firstNonBlank(incoming.getDescription(), existing.getDescription()))
                .rootCause(firstNonBlank(incoming.getRootCause(), existing.getRootCause()))
                .productionImpact(firstNonBlank(incoming.getProductionImpact(), existing.getProductionImpact()))
                .recommendation(firstNonBlank(incoming.getRecommendation(), existing.getRecommendation()))
                .fixedExample(firstNonBlank(incoming.getFixedExample(), existing.getFixedExample()))
                .confidence(Math.max(
                        existing.getConfidence() != null ? existing.getConfidence() : 0,
                        incoming.getConfidence() != null ? incoming.getConfidence() : 0))
                .estimatedFixTime(SeverityUtils.estimatedFixTime(severity))
                .autoFixAvailable(Boolean.TRUE.equals(existing.getAutoFixAvailable())
                        || Boolean.TRUE.equals(incoming.getAutoFixAvailable()))
                .mergedFrom(mergedFrom)
                .owaspCategory(firstNonBlank(incoming.getOwaspCategory(), existing.getOwaspCategory()))
                .cwe(firstNonBlank(incoming.getCwe(), existing.getCwe()))
                .cvss(incoming.getCvss() != null ? incoming.getCvss() : existing.getCvss())
                .exploitability(firstNonBlank(incoming.getExploitability(), existing.getExploitability()))
                .fixPriority(firstNonBlank(incoming.getFixPriority(), existing.getFixPriority()))
                .runtimeType(firstNonBlank(incoming.getRuntimeType(), existing.getRuntimeType()))
                .build();
    }

    private CanonicalFinding toCanonical(ReviewIssue issue, String tab, String fingerprint, String patternKey) {
        String severity = SeverityUtils.normalize(issue.getSeverity());
        String category = issue.getCategory() != null ? issue.getCategory() : "GENERAL";

        CanonicalFinding.CanonicalFindingBuilder builder = CanonicalFinding.builder()
                .id(issue.getId() != null ? issue.getId() : UUID.randomUUID().toString())
                .fingerprint(fingerprint)
                .title(issue.getTitle())
                .severity(severity)
                .severityColor(SeverityUtils.colorFor(severity))
                .category(category)
                .tab(tab)
                .file(issue.getFile())
                .line(issue.getLine())
                .description(firstNonBlank(issue.getDescription(), issue.getTitle()))
                .rootCause(firstNonBlank(issue.getRootCause(), issue.getTitle()))
                .productionImpact(issue.getProductionImpact())
                .recommendation(issue.getFixRecommendation())
                .fixedExample(issue.getFixedCodeExample())
                .confidence(issue.getConfidenceScore() != null ? issue.getConfidenceScore() : 70)
                .estimatedFixTime(SeverityUtils.estimatedFixTime(severity))
                .autoFixAvailable(issue.getFixedCodeExample() != null && !issue.getFixedCodeExample().isBlank())
                .blocking(SeverityUtils.isBlocking(severity))
                .mergedFrom(new ArrayList<>(List.of(category)));

        enrichSecurity(builder, patternKey, severity, tab);

        return builder.build();
    }

    private void enrichSecurity(
            CanonicalFinding.CanonicalFindingBuilder builder,
            String patternKey,
            String severity,
            String tab) {
        switch (patternKey) {
            case "sql_injection" -> builder
                    .owaspCategory("A03:2021-Injection")
                    .cwe("CWE-89")
                    .cvss(9.8)
                    .exploitability("HIGH")
                    .fixPriority("P0");
            case "hardcoded_secret" -> builder
                    .owaspCategory("A07:2021-Identification and Authentication Failures")
                    .cwe("CWE-798")
                    .cvss(8.1)
                    .exploitability("MEDIUM")
                    .fixPriority("P0");
            case "xss" -> builder
                    .owaspCategory("A03:2021-Injection")
                    .cwe("CWE-79")
                    .cvss(7.4)
                    .exploitability("MEDIUM")
                    .fixPriority("P1");
            default -> {
                if ("SECURITY".equals(tab)) {
                    builder.fixPriority("CRITICAL".equals(severity) ? "P0" : "P1");
                }
            }
        }
        enrichRuntime(builder, patternKey);
    }

    private void enrichRuntime(CanonicalFinding.CanonicalFindingBuilder builder, String patternKey) {
        String runtimeType = switch (patternKey) {
            case "null_assignment", "null_dereference", "unsafe_equals" -> "NullPointer";
            case "empty_catch" -> "Exception handling";
            case "resource_leak" -> "Resource leak";
            case "thread_safety" -> "Thread safety";
            case "infinite_recursion" -> "Infinite recursion";
            case "memory_leak" -> "Memory leak";
            default -> null;
        };
        if (runtimeType != null) {
            builder.runtimeType(runtimeType);
        }
    }

    String resolveTab(ReviewIssue issue) {
        String category = issue.getCategory() != null ? issue.getCategory().toUpperCase(Locale.ROOT) : "";
        String title = issue.getTitle() != null ? issue.getTitle().toLowerCase(Locale.ROOT) : "";

        if ("SECURITY".equals(category) || detectPatternKey(issue).startsWith("sql_")
                || detectPatternKey(issue).equals("hardcoded_secret")
                || detectPatternKey(issue).equals("xss")) {
            return "SECURITY";
        }
        if ("NPE".equals(category) || "RUNTIME_RISK".equals(category)) {
            return "RUNTIME";
        }
        if ("PERFORMANCE".equals(category)) {
            return "PERFORMANCE";
        }
        if ("ARCHITECTURE".equals(category)) {
            return "ARCHITECTURE";
        }
        if ("TESTING".equals(category)) {
            return "TESTING";
        }
        if ("FUNCTIONAL".equals(category) || "DATABASE".equals(category)) {
            return "RUNTIME";
        }
        if ("CODE_QUALITY".equals(category) || "STATIC_ANALYSIS".equals(category)
                || "LOGGING".equals(category) || "QUALITY".equals(category) || "AI_REVIEW".equals(category)) {
            if (title.contains("security") || title.contains("injection") || title.contains("secret")) {
                return "SECURITY";
            }
            if (title.contains("null") || title.contains("runtime") || title.contains("leak")) {
                return "RUNTIME";
            }
            if (title.contains("performance") || title.contains("slow")) {
                return "PERFORMANCE";
            }
            return "MAINTAINABILITY";
        }
        return "MAINTAINABILITY";
    }

    String detectPatternKey(ReviewIssue issue) {
        String title = issue.getTitle() != null ? issue.getTitle().toLowerCase(Locale.ROOT) : "";
        String desc = issue.getDescription() != null ? issue.getDescription().toLowerCase(Locale.ROOT) : "";
        String combined = title + " " + desc;

        if (combined.contains("sql injection") || combined.contains("string concatenation") && combined.contains("sql")) {
            return "sql_injection";
        }
        if (combined.contains("hardcoded") || combined.contains("secret") || combined.contains("api key")
                || combined.contains("password")) {
            return "hardcoded_secret";
        }
        if (combined.contains("xss") || combined.contains("innerhtml")) {
            return "xss";
        }
        if (combined.contains("= null") || combined.contains("null assignment") || combined.contains("assigned null")) {
            return "null_assignment";
        }
        if (combined.contains("nullpointer") || combined.contains("null pointer")
                || combined.contains("null check") || combined.contains("dereference")) {
            return "null_dereference";
        }
        if (combined.contains("equalsignorecase")) {
            return "unsafe_equals";
        }
        if (combined.contains("system.out") || combined.contains("system.err")) {
            return "console_logging";
        }
        if (combined.contains("empty catch") || combined.contains("swallowed exception")) {
            return "empty_catch";
        }
        if (combined.contains("thread") && combined.contains("safe")) {
            return "thread_safety";
        }
        if (combined.contains("resource leak") || combined.contains("not closed")) {
            return "resource_leak";
        }
        if (combined.contains("recursion")) {
            return "infinite_recursion";
        }
        if (combined.contains("memory leak") || combined.contains("oom")) {
            return "memory_leak";
        }
        if (combined.contains("missing test") || combined.contains("no test")) {
            return "missing_test";
        }
        if (combined.contains("large") && combined.contains("method")) {
            return "large_method";
        }

        return normalizeText(issue.getTitle());
    }

    private String fingerprint(String tab, String file, Integer line, String patternKey) {
        String payload = tab + "|" + nullSafe(file) + "|" + (line != null ? line : 0) + "|" + patternKey;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 16);
        } catch (NoSuchAlgorithmException ex) {
            return Integer.toHexString(payload.hashCode());
        }
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "unknown";
        }
        return value.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }

    private String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        return fallback;
    }

    @Data
    @Builder
    public static class NormalizationResult {
        private List<CanonicalFinding> findings;
        private int rawCount;
        private int deduplicatedCount;
    }
}
