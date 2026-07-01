package com.mergewise.service;

public final class GitHubTokenResolver {

    private GitHubTokenResolver() {
    }

    /**
     * Priority: request body token → Authorization header → server GITHUB_TOKEN env.
     */
    public static String resolve(String bodyToken, String authorizationHeader, String serverToken) {
        if (bodyToken != null && !bodyToken.isBlank()) {
            return bodyToken.trim();
        }

        String headerToken = extractFromAuthorizationHeader(authorizationHeader);
        if (headerToken != null && !headerToken.isBlank()) {
            return headerToken.trim();
        }

        if (serverToken != null && !serverToken.isBlank()) {
            return serverToken.trim();
        }

        return null;
    }

    static String extractFromAuthorizationHeader(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return null;
        }

        String value = authorizationHeader.trim();
        if (value.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return value.substring(7).trim();
        }
        if (value.regionMatches(true, 0, "token ", 0, 6)) {
            return value.substring(6).trim();
        }

        return value;
    }
}
