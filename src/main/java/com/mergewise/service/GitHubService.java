package com.mergewise.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GitHubService {

    private final WebClient webClient;

    @Value("${github.token:}")
    private String token;

    public List<String> fetchFiles(String repo, Integer prNumber) {

        String url = "https://api.github.com/repos/" + repo +
                "/pulls/" + prNumber + "/files?per_page=100";
        WebClient.RequestHeadersSpec<?> request = webClient.get()
                .uri(url)
                .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .header(HttpHeaders.USER_AGENT, "mergewise")
                .header("X-GitHub-Api-Version", "2022-11-28");

        if (token != null && !token.isBlank()) {
            request = request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token.trim());
        }

        List<GitHubFileResponse> response;
        try {
            response = request.retrieve()
                    .bodyToFlux(GitHubFileResponse.class)
                    .collectList()
                    .block();
        } catch (WebClientResponseException.NotFound ex) {
            throw new IllegalArgumentException(
                    "GitHub returned 404 for " + repo + " pull request #" + prNumber
                            + ". Check that the repository and PR exist, and that GITHUB_TOKEN has access if the repository is private.",
                    ex);
        } catch (WebClientResponseException ex) {
            throw new IllegalStateException(
                    "GitHub API request failed with " + ex.getStatusCode() + " for " + repo + " pull request #" + prNumber,
                    ex);
        }

        if (response == null || response.isEmpty()) {
            throw new RuntimeException("No files found for PR: " + prNumber);
        }

        return response.stream()
                .map(GitHubFileResponse::getPatch)   // diff content
                .filter(Objects::nonNull)           // ignore binary files
                .collect(Collectors.toList());
    }

    // DTO inside same file for simplicity
    @Data
    public static class GitHubFileResponse {
        private String filename;
        private String patch;   // THIS is what we need for AI analysis
    }
}
