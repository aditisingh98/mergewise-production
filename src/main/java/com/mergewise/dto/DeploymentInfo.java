package com.mergewise.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeploymentInfo {

    private String apiBaseUrl;

    private String analyzeEndpoint;

    private String healthCheckUrl;

    private String swaggerUrl;

    private String environment;
}
