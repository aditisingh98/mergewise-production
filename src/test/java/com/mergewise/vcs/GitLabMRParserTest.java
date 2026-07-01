package com.mergewise.vcs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GitLabMRParserTest {

    @Test
    void parsesModernGitLabUrl() {
        GitLabMergeRequestRef ref = GitLabMRParser.parse(
                "https://gitlab.intelligrape.net/bharti-axa/proposal-service/-/merge_requests/7485");

        assertEquals("gitlab.intelligrape.net", ref.host());
        assertEquals("bharti-axa/proposal-service", ref.projectPath());
        assertEquals(7485, ref.mergeRequestIid());
        assertEquals("https://gitlab.intelligrape.net/api/v4", ref.apiBaseUrl());
    }

    @Test
    void parsesGitLabComUrl() {
        GitLabMergeRequestRef ref = GitLabMRParser.parse(
                "https://gitlab.com/group/subgroup/project/-/merge_requests/42");

        assertEquals("gitlab.com", ref.host());
        assertEquals("group/subgroup/project", ref.projectPath());
        assertEquals(42, ref.mergeRequestIid());
    }

    @Test
    void rejectsInvalidUrl() {
        assertThrows(IllegalArgumentException.class,
                () -> GitLabMRParser.parse("https://github.com/owner/repo/pull/1"));
    }
}
