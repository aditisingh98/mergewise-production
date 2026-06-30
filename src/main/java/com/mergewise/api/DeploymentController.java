package com.mergewise.api;

import com.mergewise.dto.DeploymentInfo;
import com.mergewise.service.DeploymentInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DeploymentController {

    private final DeploymentInfoService deploymentInfoService;

    @GetMapping("/deployment")
    public DeploymentInfo deploymentInfo() {
        return deploymentInfoService.build();
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "mergewise");
    }
}
