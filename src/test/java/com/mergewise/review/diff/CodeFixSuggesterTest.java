package com.mergewise.review.diff;

import com.mergewise.dto.review.ReviewIssueModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CodeFixSuggesterTest {

    private final CodeFixSuggester suggester = new CodeFixSuggester();

    @Test
    void suggestsDistinctFixForWildcardImport() {
        String issue = "import java.util.*;";
        String fix = suggester.suggest(ReviewIssueModel.builder()
                .title("Wildcard import")
                .category("STATIC_ANALYSIS")
                .build(), issue);
        assertNotEquals(issue, fix);
    }

    @Test
    void suggestsLoggerInsteadOfSystemOut() {
        String issue = "System.out.println(\"Processing payment for: \" + customerId);";
        String fix = suggester.suggest(ReviewIssueModel.builder()
                .title("Uses System.out")
                .category("STATIC_ANALYSIS")
                .build(), issue);
        assertEquals("log.info(\"Processing payment for: \" + customerId);", fix);
    }

    @Test
    void fileLevelIssueHasNoIssueCode() {
        ReviewIssueModel issue = ReviewIssueModel.builder()
                .category("TESTING")
                .line(0)
                .title("Missing unit test coverage")
                .build();
        assertNull(suggester.resolveIssueCode(issue));
    }
}
