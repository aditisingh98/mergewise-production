package com.mergewise.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PRRequest {

    private String repo;

    private Integer prNumber;

    /** GitHub pull request or GitLab merge request URL (required). */
    private String prUrl;

    /** Optional GitHub PAT for private repositories. */
    @JsonProperty("githubToken")
    private String githubToken;

    /** Optional GitLab PAT for private merge requests. */
    @JsonProperty("gitlabToken")
    private String gitlabToken;

    /** Optional unified token when githubToken/gitlabToken is not set. */
    @JsonProperty("accessToken")
    private String accessToken;
}
