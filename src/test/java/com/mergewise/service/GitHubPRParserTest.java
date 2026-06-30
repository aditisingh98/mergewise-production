package com.mergewise.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GitHubPRParserTest {

    @Test
    void extractsRepoAndPrNumber() {
        String url = "https://github.com/owner/repo/pull/42";
        assertEquals("owner/repo", GitHubPRParser.extractRepo(url));
        assertEquals(42, GitHubPRParser.extractPRNumber(url));
    }

    @Test
    void rejectsInvalidHost() {
        assertThrows(IllegalArgumentException.class,
                () -> GitHubPRParser.extractRepo("https://gitlab.com/owner/repo/pull/1"));
    }
}
