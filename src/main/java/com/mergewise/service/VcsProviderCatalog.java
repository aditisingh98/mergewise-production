package com.mergewise.service;

import com.mergewise.dto.VcsProviderInfo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VcsProviderCatalog {

    public List<VcsProviderInfo> listSupportedProviders() {
        return List.of(
                VcsProviderInfo.builder()
                        .provider("GITHUB")
                        .urlPattern("https://github.com/{owner}/{repo}/pull/{number}")
                        .exampleUrl("https://github.com/owner/repo/pull/123")
                        .tokenField("githubToken")
                        .alternateTokenField("accessToken")
                        .publicAccessSupported(true)
                        .publicRequestExample("""
                                {
                                  "prUrl": "https://github.com/owner/repo/pull/123"
                                }""")
                        .privateRequestExample("""
                                {
                                  "prUrl": "https://github.com/owner/private-repo/pull/5",
                                  "githubToken": "ghp_..."
                                }""")
                        .build(),
                VcsProviderInfo.builder()
                        .provider("GITLAB")
                        .urlPattern("https://{gitlab-host}/{group}/{project}/-/merge_requests/{iid}")
                        .exampleUrl("https://gitlab.example.com/group/project/-/merge_requests/123")
                        .tokenField("gitlabToken")
                        .alternateTokenField("accessToken")
                        .publicAccessSupported(true)
                        .publicRequestExample("""
                                {
                                  "prUrl": "https://gitlab.com/group/project/-/merge_requests/123"
                                }""")
                        .privateRequestExample("""
                                {
                                  "prUrl": "https://gitlab.example.com/group/private-project/-/merge_requests/7485",
                                  "gitlabToken": "glpat-..."
                                }""")
                        .build()
        );
    }
}
