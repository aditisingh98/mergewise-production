package com.mergewise.api;

import com.mergewise.dto.PRAnalysisResponse;
import com.mergewise.dto.PRRequest;
import com.mergewise.service.PRAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pr")
@RequiredArgsConstructor
@Tag(name = "Pull Request Analysis", description = "Enterprise AI-powered PR review APIs")
public class PRController {

    private final PRAnalysisService prAnalysisService;

    @PostMapping("/analyze")
    @Operation(
            summary = "Analyze a GitHub pull request",
            description = "Runs multi-agent enterprise review. Public repos need only prUrl. "
                    + "For private repos, send githubToken in the body or Authorization: Bearer <token>."
    )
    public PRAnalysisResponse analyze(
            @Valid @RequestBody PRRequest req,
            @Parameter(
                    name = "Authorization",
                    in = ParameterIn.HEADER,
                    description = "Optional GitHub PAT: Bearer <token> (for private repositories)"
            )
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return prAnalysisService.analyze(req, authorization);
    }
}
