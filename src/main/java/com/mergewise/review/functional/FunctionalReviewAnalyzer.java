package com.mergewise.review.functional;

import com.mergewise.context.AgentContext;
import com.mergewise.dto.PRFileChange;
import com.mergewise.review.core.AddedLine;
import com.mergewise.review.core.PatchLineScanner;
import com.mergewise.review.core.ReviewAnalyzer;
import com.mergewise.review.core.ReviewIssueFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

@Component
public class FunctionalReviewAnalyzer implements ReviewAnalyzer {

    private static final Pattern API_MAPPING = Pattern.compile("@(Get|Post|Put|Patch|Delete)Mapping");
    private static final Pattern REQUEST_BODY = Pattern.compile("@RequestBody|@PathVariable|@RequestParam");
    private static final Pattern DEPRECATED = Pattern.compile("@Deprecated|deprecated");
    private static final Pattern BREAKING_RENAME = Pattern.compile("rename|breaking|BREAKING");

    @Override
    public String category() {
        return "FUNCTIONAL";
    }

    @Override
    public void analyze(AgentContext context) {
        for (PRFileChange file : context.getFileChanges()) {
            scanFile(context, file);
        }
    }

    private void scanFile(AgentContext context, PRFileChange file) {
        List<AddedLine> lines = PatchLineScanner.scanAddedLines(file);
        boolean hasApiChange = false;
        boolean hasDeprecated = false;

        for (AddedLine line : lines) {
            String code = line.getCode();

            if (API_MAPPING.matcher(code).find() || REQUEST_BODY.matcher(code).find()) {
                hasApiChange = true;
                context.getReviewIssues().add(ReviewIssueFactory.issue(
                        "FUNCTIONAL", "HIGH", line.getFilename(), line.getPatchLineNumber(),
                        "API contract change detected",
                        "New or modified REST endpoint or request binding",
                        code,
                        "Clients depending on previous API behavior may break after deployment",
                        "Document API changes, version endpoints if breaking, and update OpenAPI specs",
                        "@GetMapping(\"/v2/resource\") // version breaking changes",
                        85));
            }

            if (DEPRECATED.matcher(code).find()) {
                hasDeprecated = true;
            }

            if (code.contains("throw new") && code.contains("Exception")) {
                context.getReviewIssues().add(ReviewIssueFactory.issue(
                        "FUNCTIONAL", "MEDIUM", line.getFilename(), line.getPatchLineNumber(),
                        "Exception flow change",
                        "New exception thrown may alter user-facing error behavior",
                        code,
                        "Unhandled exceptions can change API responses and user flows",
                        "Map exceptions to stable error responses and add integration tests",
                        "throw new ResponseStatusException(HttpStatus.BAD_REQUEST, \"Invalid input\");",
                        72));
            }

            if (BREAKING_RENAME.matcher(code).find()) {
                context.getReviewIssues().add(ReviewIssueFactory.issue(
                        "FUNCTIONAL", "HIGH", line.getFilename(), line.getPatchLineNumber(),
                        "Potential backward-incompatible change",
                        "Code references breaking or rename semantics",
                        code,
                        "Existing integrations may fail after merge",
                        "Provide migration path, feature flags, or compatibility shims",
                        "// Maintain old method delegating to new implementation",
                        78));
            }
        }

        if (hasDeprecated && hasApiChange) {
            context.getMetadata().put("backwardCompatibilityRisk", "HIGH");
        } else if (hasApiChange) {
            context.getMetadata().put("backwardCompatibilityRisk", "MEDIUM");
        }
    }
}
