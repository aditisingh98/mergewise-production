package com.mergewise.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GitHubTokenResolverTest {

    @Test
    void prefersBodyTokenOverHeaderAndServer() {
        String resolved = GitHubTokenResolver.resolve(
                "body-token",
                "Bearer header-token",
                "server-token");
        assertEquals("body-token", resolved);
    }

    @Test
    void usesAuthorizationBearerWhenBodyMissing() {
        String resolved = GitHubTokenResolver.resolve(
                null,
                "Bearer header-token",
                "server-token");
        assertEquals("header-token", resolved);
    }

    @Test
    void usesServerTokenWhenRequestHasNoToken() {
        String resolved = GitHubTokenResolver.resolve(null, null, "server-token");
        assertEquals("server-token", resolved);
    }

    @Test
    void returnsNullWhenNoTokenAnywhere() {
        assertNull(GitHubTokenResolver.resolve(null, null, null));
    }

    @Test
    void supportsGitHubTokenPrefix() {
        String resolved = GitHubTokenResolver.resolve(
                null,
                "token ghp_abc123",
                null);
        assertEquals("ghp_abc123", resolved);
    }
}
