package com.mergewise.vcs;

import java.net.URI;

public final class GitLabMRParser {

    private GitLabMRParser() {
    }

    public static GitLabMergeRequestRef parse(String mrUrl) {
        URI uri = URI.create(mrUrl);
        String path = uri.getPath();

        if (path == null || path.isBlank()) {
            throw invalidFormat();
        }

        String projectPath;
        String iidPart;

        if (path.contains("/-/merge_requests/")) {
            String[] parts = path.split("/-/merge_requests/");
            projectPath = stripLeadingSlash(parts[0]);
            iidPart = parts[1];
        } else if (path.contains("/merge_requests/")) {
            String[] parts = path.split("/merge_requests/");
            projectPath = stripLeadingSlash(parts[0]);
            iidPart = parts[1];
        } else {
            throw invalidFormat();
        }

        if (projectPath.isBlank()) {
            throw invalidFormat();
        }

        String iid = iidPart.split("/")[0].trim();
        if (!iid.matches("\\d+")) {
            throw invalidFormat();
        }

        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw invalidFormat();
        }

        String scheme = uri.getScheme() != null ? uri.getScheme() : "https";
        return new GitLabMergeRequestRef(scheme, uri.getHost(), projectPath, Integer.parseInt(iid));
    }

    private static String stripLeadingSlash(String value) {
        if (value == null) {
            return "";
        }
        return value.startsWith("/") ? value.substring(1) : value;
    }

    private static IllegalArgumentException invalidFormat() {
        return new IllegalArgumentException(
                "GitLab URL must look like https://gitlab.example.com/group/project/-/merge_requests/123");
    }
}
