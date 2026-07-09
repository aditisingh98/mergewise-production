package com.mergewise.review.normalize;

import com.mergewise.dto.ReviewIssue;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FindingNormalizerTest {

    private final FindingNormalizer normalizer = new FindingNormalizer();

    @Test
    void deduplicatesNullIssuesFromMultipleSources() {
        List<ReviewIssue> raw = List.of(
                ReviewIssue.builder()
                        .severity("MEDIUM")
                        .category("STATIC_ANALYSIS")
                        .title("Possible null assignment")
                        .file("src/Main.java")
                        .line(10)
                        .description("= null detected")
                        .build(),
                ReviewIssue.builder()
                        .severity("HIGH")
                        .category("NPE")
                        .title("Null pointer risk")
                        .file("src/Main.java")
                        .line(10)
                        .description("assigned null")
                        .build(),
                ReviewIssue.builder()
                        .severity("HIGH")
                        .category("RUNTIME_RISK")
                        .title("Runtime null dereference")
                        .file("src/Main.java")
                        .line(10)
                        .description("null assignment in code")
                        .build());

        FindingNormalizer.NormalizationResult result = normalizer.normalize(raw);

        assertEquals(3, result.getRawCount());
        assertEquals(1, result.getDeduplicatedCount());
        assertEquals("HIGH", result.getFindings().get(0).getSeverity());
        assertEquals("RUNTIME", result.getFindings().get(0).getTab());
        assertTrue(result.getFindings().get(0).getMergedFrom().size() >= 2);
    }

    @Test
    void routesSqlInjectionToSecurityTab() {
        List<ReviewIssue> raw = List.of(
                ReviewIssue.builder()
                        .severity("HIGH")
                        .category("STATIC_ANALYSIS")
                        .title("SQL string concatenation")
                        .file("Repo.java")
                        .line(5)
                        .description("SQL built via string concatenation")
                        .build(),
                ReviewIssue.builder()
                        .severity("CRITICAL")
                        .category("SECURITY")
                        .title("SQL injection risk")
                        .file("Repo.java")
                        .line(5)
                        .description("SQL injection via concatenation")
                        .build());

        FindingNormalizer.NormalizationResult result = normalizer.normalize(raw);

        assertEquals(1, result.getDeduplicatedCount());
        assertEquals("SECURITY", result.getFindings().get(0).getTab());
        assertEquals("CRITICAL", result.getFindings().get(0).getSeverity());
        assertEquals("CWE-89", result.getFindings().get(0).getCwe());
    }

    @Test
    void keepsDistinctIssuesOnDifferentLines() {
        List<ReviewIssue> raw = List.of(
                ReviewIssue.builder()
                        .severity("LOW")
                        .category("LOGGING")
                        .title("System.out usage")
                        .file("App.java")
                        .line(1)
                        .build(),
                ReviewIssue.builder()
                        .severity("LOW")
                        .category("LOGGING")
                        .title("System.out usage")
                        .file("App.java")
                        .line(20)
                        .build());

        FindingNormalizer.NormalizationResult result = normalizer.normalize(raw);

        assertEquals(2, result.getDeduplicatedCount());
    }
}
