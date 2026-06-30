package com.mergewise.api;

import com.mergewise.dto.PRAnalysisResponse;
import com.mergewise.dto.PRRequest;
import com.mergewise.service.PRAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
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
    @Operation(summary = "Analyze a GitHub pull request", description = "Runs multi-agent enterprise review and returns scores, suggestions, and merge decision")
    public PRAnalysisResponse analyze(@Valid @RequestBody PRRequest req) {
        return prAnalysisService.analyze(req);
    }
}
