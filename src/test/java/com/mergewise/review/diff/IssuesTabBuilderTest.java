package com.mergewise.review.diff;

import com.mergewise.dto.PRFileChange;
import com.mergewise.dto.review.ReviewIssueModel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class IssuesTabBuilderTest {

    private final IssuesTabBuilder builder = new IssuesTabBuilder(new CodeFixSuggester());

    @Test
    void buildsDistinctIssueAndFixCode() {
        ReviewIssueModel issue = ReviewIssueModel.builder()
                .id("1")
                .severity("LOW")
                .title("Wildcard import hides which types are used.")
                .description("Wildcard import hides which types are used.")
                .file("Main.java")
                .line(2)
                .newCode("import java.util.*;")
                .recommendation("Replace wildcard imports with explicit imports for used classes.")
                .build();

        PRFileChange change = new PRFileChange();
        change.setFilename("Main.java");
        change.setStatus("modified");
        change.setRemovedLines(List.of());
        change.setAddedLines(List.of("import java.util.*;"));

        var tab = builder.build(List.of(change), List.of(issue));

        assertEquals(1, tab.getItems().size());
        assertEquals("import java.util.*;", tab.getItems().get(0).getIssueCode());
        assertNotNull(tab.getItems().get(0).getFixCode());
        assertNotEquals(tab.getItems().get(0).getIssueCode(), tab.getItems().get(0).getFixCode());
        assertEquals(1, tab.getFileDiffs().size());
        assertEquals(List.of("1"), tab.getFileDiffs().get(0).getIssueIds());
    }
}
