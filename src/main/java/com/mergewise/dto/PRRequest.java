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
            description = "Optional GitHub PAT for private repositories. "
                    + "Can also be sent via Authorization: Bearer <token>. "
                    + "Public repos work without a token.",
            example = "ghp_xxxxxxxxxxxx"
    )
    @JsonProperty("githubToken")
    private String githubToken;
}