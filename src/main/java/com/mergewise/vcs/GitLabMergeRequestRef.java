package com.mergewise.vcs;

public record GitLabMergeRequestRef(
        String scheme,
        String host,
        String projectPath,
        Integer mergeRequestIid
) {
    public String apiBaseUrl() {
        return scheme + "://" + host + "/api/v4";
    }
}
