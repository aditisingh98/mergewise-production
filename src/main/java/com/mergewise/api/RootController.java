package com.mergewise.api;

import com.mergewise.dto.DeploymentInfo;
import com.mergewise.service.DeploymentInfoService;
import com.mergewise.service.VcsProviderCatalog;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class RootController {

    private final DeploymentInfoService deploymentInfoService;
    private final VcsProviderCatalog vcsProviderCatalog;

    @GetMapping("/")
    public Map<String, Object> root() {
        DeploymentInfo info = deploymentInfoService.build();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("service", "MergeWise AI Review API");
        body.put("status", "UP");
        body.put("version", "1.0.0");
        body.put("providersEndpoint", info.getApiBaseUrl() + "/api/pr/providers");
        body.put("supportedProviders", vcsProviderCatalog.listSupportedProviders());
        body.put("analyze", Map.of(
                "method", "POST",
                "url", info.getAnalyzeEndpoint(),
                "note", "Public GitHub/GitLab: prUrl only. Private: add githubToken or gitlabToken (or accessToken)."
        ));
        body.put("health", info.getHealthCheckUrl());
        body.put("swagger", info.getSwaggerUrl());
        body.put("deployment", info);
        return body;
    }
}
