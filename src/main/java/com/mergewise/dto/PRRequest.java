package com.mergewise.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PRRequest {
 private String repo;
 private Integer prNumber;
 @NotBlank(message = "prUrl is required")
 private String prUrl;

}