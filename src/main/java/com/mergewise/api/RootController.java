package com.mergewise.api;

import com.mergewise.dto.DeploymentInfo;
import com.mergewise.service.DeploymentInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class RootController {

    private final DeploymentInfoService deploymentInfoService;

    @GetMapping("/")
    public Map<String, Object> root() {
        DeploymentInfo info = deploymentInfoService.build();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("service", "MergeWise AI Review API");
        body.put("status", "UP");
        body.put("version", "1.0.0");
        body.put("analyze", Map.of(
                "method", "POST",
                "url", info.getAnalyzeEndpoint(),
                "body", Map.of(
                        "prUrl", "https://github.com/owner/repo/pull/123",
                        "githubToken", "(optional — required for private repos)"
                ),
                "headers", Map.of(
                        "Authorization", "Bearer <github-token> (optional alternative to githubToken)"
                )
        ));
        body.put("health", info.getHealthCheckUrl());
        body.put("swagger", info.getSwaggerUrl());
        body.put("deployment", info);
        return body;
    }
}
