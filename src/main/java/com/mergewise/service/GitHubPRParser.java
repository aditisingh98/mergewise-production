package com.mergewise.service;

import java.net.URI;

public class GitHubPRParser {

    public static String extractRepo(String prUrl) {
        String[] parts = parsePathParts(prUrl);
        return parts[0] + "/" + parts[1];
    }

    public static Integer extractPRNumber(String prUrl) {
        String[] parts = parsePathParts(prUrl);
        return Integer.parseInt(parts[3]);
    }

    private static String[] parsePathParts(String prUrl) {
        URI uri = URI.create(prUrl);

        if (!"github.com".equalsIgnoreCase(uri.getHost())) {
            throw new IllegalArgumentException("PR URL must be a github.com pull request URL");
        }

        String[] parts = uri.getPath().split("/");
        if (parts.length < 5 || !"pull".equals(parts[3])) {
            throw new IllegalArgumentException("PR URL must look like https://github.com/{owner}/{repo}/pull/{number}");
        }

        return new String[] { parts[1], parts[2], parts[3], parts[4] };
    }
}