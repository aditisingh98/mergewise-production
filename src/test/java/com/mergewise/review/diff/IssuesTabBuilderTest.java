package com.mergewise.review.diff;

import com.mergewise.dto.PRFileChange;
import com.mergewise.dto.review.ReviewIssueModel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class IssuesTabBuilderTest {

    private final IssuesTabBuilder builder = new IssuesTabBuilder();

    @Test
    void buildsIssueCodeAndFixCodePerItem() {
        ReviewIssueModel issue = ReviewIssueModel.builder()
                .id("1")
                .severity("HIGH")
                .title("Null risk")
                .description("Dereference without check")
                .file("Main.java")
                .line(10)
                .issueCode("value.length();")
                .fixCode("if (value != null) { value.length(); }")
                .recommendation("Add null guard")
                .build();

        PRFileChange change = new PRFileChange();
        change.setFilename("Main.java");
        change.setStatus("modified");
        change.setRemovedLines(List.of("log.info(\"start\");"));
        change.setAddedLines(List.of("value.length();"));

        var tab = builder.build(List.of(change), List.of(issue));

        assertEquals(1, tab.getItems().size());
        assertEquals("value.length();", tab.getItems().get(0).getIssueCode());
        assertEquals("if (value != null) { value.length(); }", tab.getItems().get(0).getFixCode());
        assertEquals(1, tab.getFileDiffs().size());
        assertEquals(1, tab.getFileDiffs().get(0).getRemovedLines().size());
        assertEquals(1, tab.getFileDiffs().get(0).getAddedLines().size());
        assertNotNull(tab.getFileDiffs().get(0).getIssues().get(0).getFixGuide());
    }
}
