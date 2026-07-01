package com.mergewise.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PRRequest {

    private String repo;

    private Integer prNumber;

    @NotBlank(message = "prUrl is required")
    private String prUrl;

    @Schema(
            description = "Optional GitHub PAT for private GitHub repositories. "
                    + "Ignored for GitLab URLs. Can also use Authorization: Bearer <token>.",
            example = "ghp_xxxxxxxxxxxx"
    )
    @JsonProperty("githubToken")
    private String githubToken;

    @Schema(
            description = "Optional GitLab personal access token for private GitLab merge requests. "
                    + "Required for most self-hosted/private GitLab projects. "
                    + "Can also use Authorization: Bearer <token>.",
            example = "glpat-xxxxxxxxxxxx"
    )
    @JsonProperty("gitlabToken")
    private String gitlabToken;

    @Schema(
            description = "Optional unified access token. Used for the detected provider when "
                    + "githubToken/gitlabToken is not set. Prefer githubToken or gitlabToken when both platforms are used.",
            example = "ghp_xxx or glpat-xxx"
    )
    @JsonProperty("accessToken")
    private String accessToken;
}