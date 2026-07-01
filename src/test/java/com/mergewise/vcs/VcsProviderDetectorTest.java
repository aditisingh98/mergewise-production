package com.mergewise.vcs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VcsProviderDetectorTest {

    @Test
    void detectsGitHub() {
        assertEquals(VcsProvider.GITHUB,
                VcsProviderDetector.detect("https://github.com/owner/repo/pull/1"));
    }

    @Test
    void detectsGitLab() {
        assertEquals(VcsProvider.GITLAB,
                VcsProviderDetector.detect(
                        "https://gitlab.intelligrape.net/bharti-axa/proposal-service/-/merge_requests/7485"));
    }

    @Test
    void rejectsUnsupportedHost() {
        assertThrows(IllegalArgumentException.class,
                () -> VcsProviderDetector.detect("https://bitbucket.org/workspace/repo/pull-requests/1"));
    }
}
