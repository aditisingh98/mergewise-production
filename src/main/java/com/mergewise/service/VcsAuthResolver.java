package com.mergewise.service;

import com.mergewise.dto.PRRequest;

public final class VcsAuthResolver {

    private VcsAuthResolver() {
    }

    public static String resolveGitHubToken(PRRequest request, String authorizationHeader, String serverToken) {
        return GitHubTokenResolver.resolve(
                firstNonBlank(request.getGithubToken(), request.getAccessToken()),
                authorizationHeader,
                serverToken);
    }

    public static String resolveGitLabToken(PRRequest request, String authorizationHeader, String serverToken) {
        return GitHubTokenResolver.resolve(
                firstNonBlank(request.getGitlabToken(), request.getAccessToken()),
                authorizationHeader,
                serverToken);
    }

    public static String authMode(String userToken, String serverToken, String effectiveToken) {
        if (effectiveToken == null) {
            return "PUBLIC";
        }
        if (userToken != null && !userToken.isBlank()) {
            return "USER_TOKEN";
        }
        if (serverToken != null && !serverToken.isBlank()
                && effectiveToken.equals(serverToken.trim())) {
            return "SERVER_TOKEN";
        }
        return "USER_TOKEN";
    }

    public static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
