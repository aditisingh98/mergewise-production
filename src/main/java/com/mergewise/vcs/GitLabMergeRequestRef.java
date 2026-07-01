package com.mergewise.vcs;

public record GitLabMergeRequestRef(
        String host,
        String projectPath,
        Integer mergeRequestIid
) {
    public String apiBaseUrl() {
        return "https://" + host + "/api/v4";
    }
}
