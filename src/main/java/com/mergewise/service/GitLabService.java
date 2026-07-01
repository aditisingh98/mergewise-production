package com.mergewise.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mergewise.dto.FileLanguageDetector;
import com.mergewise.dto.PRFileChange;
import com.mergewise.vcs.GitLabMergeRequestRef;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GitLabService {

    private final WebClient webClient;

    @Value("${gitlab.token:}")
    private String serverToken;

    public List<PRFileChange> fetchFileChanges(
            GitLabMergeRequestRef ref,
            String bodyToken,
            String authorizationHeader) {

        String effectiveToken = GitHubTokenResolver.resolve(bodyToken, authorizationHeader, serverToken);
        String encodedProject = URLEncoder.encode(ref.projectPath(), StandardCharsets.UTF_8);
        String url = ref.apiBaseUrl() + "/projects/" + encodedProject
                + "/merge_requests/" + ref.mergeRequestIid() + "/changes";

        WebClient.RequestHeadersSpec<?> request = webClient.get()
                .uri(url)
                .header(HttpHeaders.ACCEPT, "application/json");

        if (effectiveToken != null) {
            request = request.header("PRIVATE-TOKEN", effectiveToken);
        }

        GitLabChangesResponse response;
        try {
            response = request.retrieve()
                    .bodyToMono(GitLabChangesResponse.class)
                    .block();
        } catch (WebClientResponseException.NotFound ex) {
            throw new IllegalArgumentException(
                    "GitLab returned 404 for " + ref.projectPath() + " merge request !"
                            + ref.mergeRequestIid()
                            + ". Check the URL, or provide gitlabToken / GITLAB_TOKEN for private projects.",
                    ex);
        } catch (WebClientResponseException.Unauthorized ex) {
            throw new IllegalArgumentException(
                    "GitLab rejected the access token (401 Unauthorized). "
                            + "Verify the token has api/read_api scope and access to " + ref.projectPath() + ".",
                    ex);
        } catch (WebClientResponseException.Forbidden ex) {
            throw new IllegalArgumentException(
                    "GitLab denied access (403 Forbidden). Provide a valid gitlabToken with access to "
                            + ref.projectPath() + ".",
                    ex);
        } catch (WebClientResponseException ex) {
            throw new IllegalStateException(
                    "GitLab API request failed with " + ex.getStatusCode() + " for "
                            + ref.projectPath() + " merge request !" + ref.mergeRequestIid(),
                    ex);
        }

        if (response == null || response.getChanges() == null || response.getChanges().isEmpty()) {
            throw new IllegalArgumentException("No file changes found for GitLab merge request !"
                    + ref.mergeRequestIid());
        }

        return response.getChanges().stream()
                .map(this::toFileChange)
                .collect(Collectors.toList());
    }

    private PRFileChange toFileChange(GitLabChange change) {
        PRFileChange fileChange = new PRFileChange();

        String filename = change.getNewPath() != null ? change.getNewPath() : change.getOldPath();
        fileChange.setFilename(filename);
        fileChange.setPreviousFilename(change.getOldPath());

        if (Boolean.TRUE.equals(change.getNewFile())) {
            fileChange.setStatus("added");
        } else if (Boolean.TRUE.equals(change.getDeletedFile())) {
            fileChange.setStatus("removed");
        } else if (Boolean.TRUE.equals(change.getRenamedFile())) {
            fileChange.setStatus("renamed");
        } else {
            fileChange.setStatus("modified");
        }

        String diff = change.getDiff() != null ? change.getDiff() : "";
        fileChange.setPatch(diff);
        fileChange.setAddedLines(extractChangedLines(diff, '+'));
        fileChange.setRemovedLines(extractChangedLines(diff, '-'));

        int additions = countLines(diff, '+');
        int deletions = countLines(diff, '-');
        fileChange.setAdditions(additions);
        fileChange.setDeletions(deletions);
        fileChange.setChanges(additions + deletions);

        fileChange.setLanguage(FileLanguageDetector.detect(filename));
        fileChange.setModule(extractModule(filename));
        fileChange.setBackendCritical(isBackendCritical(filename));
        fileChange.setRiskScore(calculateInitialRiskScore(fileChange));

        return fileChange;
    }

    private String extractModule(String filename) {
        if (filename == null) {
            return "unknown";
        }
        String[] parts = filename.split("/");
        return parts.length > 2 ? parts[2] : "root";
    }

    private Boolean isBackendCritical(String filename) {
        if (filename == null) {
            return false;
        }
        String lower = filename.toLowerCase();
        return lower.contains("controller") || lower.contains("service")
                || lower.contains("repository") || lower.contains("config")
                || lower.contains("security") || lower.contains("kafka");
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

    private int countLines(String patch, char marker) {
        int count = 0;
        for (String line : patch.split("\\R")) {
            if (line.length() >= 2 && line.charAt(0) == marker
                    && !line.startsWith("+++") && !line.startsWith("---")) {
                count++;
            }
        }
        return count;
    }

    @Data
    public static class GitLabChangesResponse {
        private List<GitLabChange> changes;
    }

    @Data
    public static class GitLabChange {
        @JsonProperty("old_path")
        private String oldPath;
        @JsonProperty("new_path")
        private String newPath;
        private String diff;
        @JsonProperty("new_file")
        private Boolean newFile;
        @JsonProperty("renamed_file")
        private Boolean renamedFile;
        @JsonProperty("deleted_file")
        private Boolean deletedFile;
    }
}
