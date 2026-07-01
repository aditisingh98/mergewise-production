package com.mergewise.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mergewise.dto.FileLanguageDetector;
import com.mergewise.dto.PRFileChange;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
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
        return fetchFileChanges(repo, prNumber).stream()
                .map(PRFileChange::getPatch)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<PRFileChange> fetchFileChanges(String repo, Integer prNumber) {
        return fetchFileChanges(repo, prNumber, null, null);
    }

    public List<PRFileChange> fetchFileChanges(
            String repo,
            Integer prNumber,
            String bodyToken,
            String authorizationHeader) {
        String effectiveToken = GitHubTokenResolver.resolve(bodyToken, authorizationHeader, token);

        String url = "https://api.github.com/repos/" + repo +
                "/pulls/" + prNumber + "/files?per_page=100";
        WebClient.RequestHeadersSpec<?> request = webClient.get()
                .uri(url)
                .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .header(HttpHeaders.USER_AGENT, "mergewise")
                .header("X-GitHub-Api-Version", "2022-11-28");

        if (effectiveToken != null) {
            request = request.header(HttpHeaders.AUTHORIZATION, "Bearer " + effectiveToken);
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
                            + ". Check that the repository and PR exist. For private repositories, "
                            + "provide githubToken in the request body or Authorization: Bearer <token>.",
                    ex);
        } catch (WebClientResponseException.Unauthorized ex) {
            throw new IllegalArgumentException(
                    "GitHub rejected the access token (401 Unauthorized). "
                            + "Verify the token has repo scope and access to " + repo + ".",
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
                .map(this::toFileChange)
                .collect(Collectors.toList());
    }

    private PRFileChange toFileChange(GitHubFileResponse response) {

        PRFileChange change = new PRFileChange();

        change.setFilename(response.getFilename());

        change.setStatus(response.getStatus());

        change.setAdditions(response.getAdditions());

        change.setDeletions(response.getDeletions());

        change.setChanges(response.getChanges());

        change.setPreviousFilename(response.getPreviousFilename());

        change.setPatch(response.getPatch());

        change.setAddedLines(
                extractChangedLines(response.getPatch(), '+')
        );

        change.setRemovedLines(
                extractChangedLines(response.getPatch(), '-')
        );

        change.setLanguage(
                FileLanguageDetector.detect(
                        change.getFilename()
                )
        );

        change.setModule(
                extractModule(
                        change.getFilename()
                )
        );

        change.setBackendCritical(
                isBackendCritical(
                        change.getFilename()
                )
        );

        change.setRiskScore(
                calculateInitialRiskScore(change)
        );

        return change;
    }

    private List<String> extractChangedLines(String patch, char marker) {
        List<String> lines = new ArrayList<>();
        if (patch == null || patch.isBlank()) {
            return lines;
        }

        for (String line : patch.split("\\R")) {
            if (line.length() < 2 || line.charAt(0) != marker) {
                continue;
            }
            if (line.startsWith("+++") || line.startsWith("---")) {
                continue;
            }
            lines.add(line.substring(1));
        }
        return lines;
    }

    // DTO inside same file for simplicity
    @Data
    public static class GitHubFileResponse {
        private String filename;
        private String status;
        private Integer additions;
        private Integer deletions;
        private Integer changes;
        @JsonProperty("previous_filename")
        private String previousFilename;
        private String patch;   // THIS is what we need for AI analysis
    }
    private String extractModule(String filename) {

        if (filename == null) {
            return "unknown";
        }

        String[] parts = filename.split("/");

        if (parts.length > 2) {
            return parts[2];
        }

        return "root";
    }
    private Boolean isBackendCritical(String filename) {

        if (filename == null) {
            return false;
        }

        filename = filename.toLowerCase();

        return filename.contains("controller")
                || filename.contains("service")
                || filename.contains("repository")
                || filename.contains("config")
                || filename.contains("security")
                || filename.contains("kafka");
    }
    private Integer calculateInitialRiskScore(PRFileChange file) {

        int score = 0;

        if (Boolean.TRUE.equals(file.getBackendCritical())) {
            score += 40;
        }

        if ("java".equalsIgnoreCase(file.getLanguage())) {
            score += 25;
        }

        if (file.getChanges() != null) {
            score += Math.min(file.getChanges(), 30);
        }

        return Math.min(score, 100);
    }
}
