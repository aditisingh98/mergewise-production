package com.mergewise.service;

import com.mergewise.dto.PRRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VcsAuthResolverTest {

    @Test
    void resolvesGitHubTokenFromAccessTokenFallback() {
        PRRequest request = new PRRequest();
        request.setAccessToken("ghp_test");

        assertEquals("ghp_test", VcsAuthResolver.resolveGitHubToken(request, null, null));
    }

    @Test
    void prefersGithubTokenOverAccessToken() {
        PRRequest request = new PRRequest();
        request.setGithubToken("ghp_primary");
        request.setAccessToken("ghp_fallback");

        assertEquals("ghp_primary", VcsAuthResolver.resolveGitHubToken(request, null, null));
    }

    @Test
    void resolvesGitLabTokenFromAccessTokenFallback() {
        PRRequest request = new PRRequest();
        request.setAccessToken("glpat-test");

        assertEquals("glpat-test", VcsAuthResolver.resolveGitLabToken(request, null, null));
    }

    @Test
    void authModePublicWhenNoToken() {
        assertEquals("PUBLIC", VcsAuthResolver.authMode(null, null, null));
    }

    @Test
    void authModeUserToken() {
        assertEquals("USER_TOKEN", VcsAuthResolver.authMode("user-token", "server-token", "user-token"));
    }

    @Test
    void authModeServerToken() {
        assertEquals("SERVER_TOKEN", VcsAuthResolver.authMode(null, "server-token", "server-token"));
    }
}
