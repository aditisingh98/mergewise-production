package com.mergewise.api;

import com.mergewise.dto.PRAnalysisResponse;
import com.mergewise.dto.PRRequest;
import com.mergewise.dto.VcsProviderInfo;
import com.mergewise.service.PRAnalysisService;
import com.mergewise.service.VcsProviderCatalog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pr")
@RequiredArgsConstructor
@Tag(name = "Pull Request Analysis", description = "GitHub and GitLab merge request review APIs")
public class PRController {

    private final PRAnalysisService prAnalysisService;
    private final VcsProviderCatalog vcsProviderCatalog;

    @GetMapping("/providers")
    @Operation(summary = "List supported VCS providers and token rules")
    public List<VcsProviderInfo> providers() {
        return vcsProviderCatalog.listSupportedProviders();
    }

    @PostMapping("/analyze")
    @Operation(
            summary = "Analyze a GitHub pull request or GitLab merge request",
            description = "Auto-detects provider from prUrl. Public repos/MRs need only prUrl. "
                    + "Private GitHub: githubToken or accessToken. Private GitLab: gitlabToken or accessToken. "
                    + "Authorization: Bearer <token> is also supported."
    )
    public PRAnalysisResponse analyze(
            @RequestBody PRRequest req,
            @Parameter(
                    name = "Authorization",
                    in = ParameterIn.HEADER,
                    description = "Optional PAT: Bearer <token> (GitHub or GitLab depending on prUrl)"
            )
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return prAnalysisService.analyze(req, authorization);
    }
}
