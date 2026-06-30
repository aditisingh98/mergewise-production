package com.mergewise.review.quality;

import com.mergewise.context.AgentContext;
import com.mergewise.dto.PRFileChange;
import com.mergewise.review.core.AddedLine;
import com.mergewise.review.core.PatchLineScanner;
import com.mergewise.review.core.ReviewAnalyzer;
import com.mergewise.review.core.ReviewIssueFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class CodeQualityReviewAnalyzer implements ReviewAnalyzer {

    private static final Pattern DEEP_NESTING = Pattern.compile("(\\{[^}]*\\}){3,}");
    private static final Pattern LONG_METHOD = Pattern.compile("(public|private|protected)\\s+\\w+[^{]+\\{");
    private static final Pattern GOD_CLASS = Pattern.compile("class\\s+\\w+");
    private static final Pattern DUPLICATE_BLOCK = Pattern.compile(".{40,}");

    @Override
    public String category() {
        return "CODE_QUALITY";
    }

    @Override
    public void analyze(AgentContext context) {
        Map<String, Integer> snippetCounts = new HashMap<>();

        for (PRFileChange file : context.getFileChanges()) {
            if (!PatchLineScanner.isJavaFile(file)) {
                continue;
            }
            analyzeFile(context, file, snippetCounts);
        }
    }

    private void analyzeFile(AgentContext context, PRFileChange file, Map<String, Integer> snippetCounts) {
        List<AddedLine> lines = PatchLineScanner.scanAddedLines(file);
        int braceDepth = 0;
        int maxDepth = 0;
        int methodLines = 0;
        boolean inMethod = false;

        for (AddedLine line : lines) {
            String code = line.getCode();

            for (char c : code.toCharArray()) {
                if (c == '{') {
                    braceDepth++;
                    maxDepth = Math.max(maxDepth, braceDepth);
                    inMethod = true;
                    methodLines = 0;
                } else if (c == '}') {
                    braceDepth = Math.max(0, braceDepth - 1);
                    if (inMethod && methodLines > 50) {
                        context.getReviewIssues().add(ReviewIssueFactory.issue(
                                "CODE_QUALITY", "MEDIUM", line.getFilename(), line.getPatchLineNumber(),
                                "Large method detected",
                                "Method body exceeds recommended size threshold",
                                "Method grew beyond ~50 added lines in patch",
                                "Large methods are harder to test and maintain",
                                "Extract helper methods or apply single-responsibility refactoring",
                                "private void validateInput(Input input) { /* focused logic */ }",
                                80));
                    }
                    inMethod = false;
                    methodLines = 0;
                }
            }
            if (inMethod) {
                methodLines++;
            }

            if (maxDepth >= 4) {
                context.getReviewIssues().add(ReviewIssueFactory.issue(
                        "CODE_QUALITY", "MEDIUM", line.getFilename(), line.getPatchLineNumber(),
                        "Deep nesting detected",
                        "Excessive control-flow nesting reduces readability",
                        code,
                        "Complex branching increases bug rate and review difficulty",
                        "Use guard clauses, early returns, or extract nested blocks",
                        "if (input == null) return; // guard clause",
                        76));
                maxDepth = 0;
            }

            if (GOD_CLASS.matcher(code).find() && file.getChanges() != null && file.getChanges() > 300) {
                context.getReviewIssues().add(ReviewIssueFactory.issue(
                        "CODE_QUALITY", "MEDIUM", line.getFilename(), line.getPatchLineNumber(),
                        "Large class change",
                        "Significant additions to a single class",
                        code,
                        "God classes violate Single Responsibility Principle",
                        "Split responsibilities into smaller cohesive classes",
                        "class OrderValidator { } // extracted concern",
                        74));
            }

            if (code.length() > 40) {
                String normalized = code.replaceAll("\\s+", " ").trim();
                snippetCounts.merge(normalized, 1, Integer::sum);
                if (snippetCounts.get(normalized) > 1) {
                    context.getReviewIssues().add(ReviewIssueFactory.issue(
                            "CODE_QUALITY", "LOW", line.getFilename(), line.getPatchLineNumber(),
                            "Possible duplicate code",
                            "Repeated code block detected across additions",
                            code,
                            "Duplication increases maintenance cost and defect risk",
                            "Extract shared logic into a reusable utility or service",
                            "private void sharedValidation() { /* reused logic */ }",
                            68));
                }
            }

            if (code.matches(".*\\b[a-z]{1,2}\\b\\s*[=;].*") && !code.contains("for") && !code.contains("i++")) {
                context.getReviewIssues().add(ReviewIssueFactory.issue(
                        "CODE_QUALITY", "LOW", line.getFilename(), line.getPatchLineNumber(),
                        "Naming convention concern",
                        "Very short variable name may reduce readability",
                        code,
                        "Unclear naming slows onboarding and reviews",
                        "Use descriptive names aligned with domain language",
                        "String customerEmail = value;",
                        60));
            }

            if (code.contains("new ") && (code.contains("Service") || code.contains("Repository"))
                    && !code.contains("@Autowired") && !code.contains("private final")) {
                context.getReviewIssues().add(ReviewIssueFactory.issue(
                        "CODE_QUALITY", "MEDIUM", line.getFilename(), line.getPatchLineNumber(),
                        "Tight coupling / manual instantiation",
                        "Direct instantiation instead of dependency injection",
                        code,
                        "Hard-wired dependencies reduce testability and flexibility",
                        "Inject dependencies via constructor and interfaces",
                        "public OrderService(OrderRepository repository) { this.repository = repository; }",
                        82));
            }
        }
    }
}
