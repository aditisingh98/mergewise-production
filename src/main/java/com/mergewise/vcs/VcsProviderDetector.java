package com.mergewise.vcs;

import java.net.URI;

public final class VcsProviderDetector {

    private VcsProviderDetector() {
    }

    public static VcsProvider detect(String url) {
        URI uri = URI.create(url);
        String host = uri.getHost() != null ? uri.getHost().toLowerCase() : "";
        String path = uri.getPath() != null ? uri.getPath() : "";

        if ("github.com".equals(host)) {
            return VcsProvider.GITHUB;
        }

        if (path.contains("merge_requests")) {
            return VcsProvider.GITLAB;
        }

        throw new IllegalArgumentException(
                "Unsupported URL. Use a GitHub pull request URL (github.com/.../pull/N) "
                        + "or a GitLab merge request URL (.../merge_requests/N).");
    }
}
