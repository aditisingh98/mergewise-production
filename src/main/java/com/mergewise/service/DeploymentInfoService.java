package com.mergewise.service;

import com.mergewise.config.PublicUrlResolver;
import com.mergewise.dto.DeploymentInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeploymentInfoService {

    private final PublicUrlResolver publicUrlResolver;

    @Value("${spring.profiles.active:default}")
    private String environment;

    public DeploymentInfo build() {
        String base = publicUrlResolver.resolve();
        return DeploymentInfo.builder()
                .apiBaseUrl(base)
                .analyzeEndpoint(base + "/api/pr/analyze")
                .healthCheckUrl(base + "/actuator/health")
                .swaggerUrl(base + "/swagger-ui.html")
                .environment(environment)
                .build();
    }
}
