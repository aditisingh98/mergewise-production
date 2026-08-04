package com.mergewise.review.diff;

import com.mergewise.dto.PRFileChange;
import com.mergewise.dto.review.DiffLine;
import com.mergewise.dto.review.ReviewIssueModel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IssueCodeContextEnricherTest {

    private final IssueCodeContextEnricher enricher = new IssueCodeContextEnricher(new PatchDiffParser());

    @Test
    void attachesOldNewCodeAndHighlightsDiffLine() {
        String patch = """
                @@ -10,3 +10,4 @@
                 public void run() {
                -    log.info("start");
                +    String value = null;
                +    value.length();
                 }
                """;
        PRFileChange change = new PRFileChange();
        change.setFilename("src/Main.java");
        change.setPatch(patch);

        ReviewIssueModel issue = ReviewIssueModel.builder()
                .id("issue-1")
                .severity("HIGH")
                .category("RUNTIME_RISK")
                .file("src/Main.java")
                .line(12)
                .title("Null dereference risk")
                .description("value may be null before length() call")
                .recommendation("Add null check or use Optional")
                .build();

        List<ReviewIssueModel> enriched = enricher.enrich(List.of(change), List.of(issue));
        ReviewIssueModel result = enriched.get(0);

        assertNotNull(result.getNewCode());
        assertTrue(result.getNewCode().contains("value.length()"));
        assertNotNull(result.getDevelopmentGuidance());
        assertNotNull(result.getCodeComparison());
        assertFalse(result.getCodeComparison().getAfter().isEmpty());

        List<DiffLine> annotated = enricher.annotateDiffWithIssues(
                new PatchDiffParser().parse(patch),
                enriched);
        assertTrue(annotated.stream().anyMatch(l -> Boolean.TRUE.equals(l.getHighlighted())));
    }

    @Test
    void cleansAiIssuePrefixFromDescription() {
        ReviewIssueModel issue = ReviewIssueModel.builder()
                .id("i2")
                .file("App.java")
                .title("SQL risk")
                .description("ISSUE: [high] App.java - SQL built via concatenation")
                .build();

        List<ReviewIssueModel> enriched = enricher.enrich(List.of(), List.of(issue));
        assertEquals("SQL built via concatenation", enriched.get(0).getDescription());
    }
}
