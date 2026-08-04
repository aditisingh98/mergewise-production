package com.mergewise.api;

import com.mergewise.dto.PRAnalysisResponse;
import com.mergewise.dto.PRRequest;
import com.mergewise.dto.VcsProviderInfo;
import com.mergewise.service.PRAnalysisService;
import com.mergewise.service.VcsProviderCatalog;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pr")
@RequiredArgsConstructor
public class PRController {

    private final PRAnalysisService prAnalysisService;
    private final VcsProviderCatalog vcsProviderCatalog;

    @GetMapping("/providers")
    public List<VcsProviderInfo> providers() {
        return vcsProviderCatalog.listSupportedProviders();
    }

    @PostMapping("/analyze")
    public PRAnalysisResponse analyze(
            @RequestBody PRRequest req,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return prAnalysisService.analyze(req, authorization);
    }
}
