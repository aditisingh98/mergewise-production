package com.mergewise.review.architecture;

import com.mergewise.context.AgentContext;
import com.mergewise.dto.ArchitectureRecommendation;
import com.mergewise.dto.PRFileChange;
import com.mergewise.review.core.AddedLine;
import com.mergewise.review.core.PatchLineScanner;
import com.mergewise.review.core.ReviewAnalyzer;
import com.mergewise.review.core.ReviewIssueFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ArchitectureReviewAnalyzer implements ReviewAnalyzer {

    @Override
    public String category() {
        return "ARCHITECTURE";
    }

    @Override
    public void analyze(AgentContext context) {
        for (PRFileChange file : context.getFileChanges()) {
            if (!PatchLineScanner.isJavaFile(file)) {
                continue;
            }
            analyzeFile(context, file);
        }
    }

    private void analyzeFile(AgentContext context, PRFileChange file) {
        List<AddedLine> lines = PatchLineScanner.scanAddedLines(file);
        String filename = file.getFilename() != null ? file.getFilename() : "unknown";

        for (AddedLine line : lines) {
            String code = line.getCode();

            if (code.contains("switch") && code.contains("case") && code.matches(".*case\\s+\\d+.*")) {
                storeRecommendation(context, ArchitectureRecommendation.builder()
                        .pattern("Strategy")
                        .file(filename)
                        .observation("Switch-based type dispatch detected")
                        .recommendation("Replace switch with Strategy pattern for extensibility")
                        .impact("Easier to add new behaviors without modifying existing switch")
                        .build());
            }

            if (code.contains("if") && code.contains("type.equals") && code.contains("else if")) {
                storeRecommendation(context, ArchitectureRecommendation.builder()
                        .pattern("Strategy")
                        .file(filename)
                        .observation("Type-based conditional branching")
                        .recommendation("Use polymorphism or Strategy pattern instead of type checks")
                        .impact("Reduces cyclomatic complexity and improves testability")
                        .build());
            }

            if (code.contains("new ") && (code.contains("Factory") || code.contains("create"))) {
                storeRecommendation(context, ArchitectureRecommendation.builder()
                        .pattern("Factory")
                        .file(filename)
                        .observation("Object creation logic in business code")
                        .recommendation("Extract Factory for complex object construction")
                        .impact("Centralizes creation rules and simplifies unit testing")
                        .build());
            }

            if ((code.contains("JdbcTemplate") || code.contains("EntityManager"))
                    && filename.toLowerCase().contains("service")) {
                storeRecommendation(context, ArchitectureRecommendation.builder()
                        .pattern("Repository")
                        .file(filename)
                        .observation("Data access mixed in service layer")
                        .recommendation("Introduce Repository abstraction for persistence")
                        .impact("Improves separation of concerns and mockability")
                        .build());

                context.getReviewIssues().add(ReviewIssueFactory.issue(
                        "ARCHITECTURE", "MEDIUM", filename, line.getPatchLineNumber(),
                        "Separation of concerns",
                        "Data access logic in service class",
                        code,
                        "Tight coupling between business and persistence layers",
                        "Move queries to a dedicated repository interface",
                        "public interface OrderRepository extends JpaRepository<Order, Long> { }",
                        80));
            }

            if (code.contains("@Autowired") && code.contains("field")) {
                context.getReviewIssues().add(ReviewIssueFactory.issue(
                        "ARCHITECTURE", "LOW", filename, line.getPatchLineNumber(),
                        "Field injection detected",
                        "Field-level @Autowired reduces testability",
                        code,
                        "Constructor injection is preferred for immutable dependencies",
                        "Use constructor injection with final fields",
                        "public OrderService(OrderRepository repo) { this.repo = repo; }",
                        70));
            }

            if (filename.contains("Controller") && code.contains("Repository")) {
                context.getReviewIssues().add(ReviewIssueFactory.issue(
                        "ARCHITECTURE", "HIGH", filename, line.getPatchLineNumber(),
                        "Layer violation",
                        "Controller directly accessing repository",
                        code,
                        "Skips service layer validation and business rules",
                        "Route through service layer; keep controllers thin",
                        "return orderService.findById(id);",
                        90));
            }
        }

        if (file.getChanges() != null && file.getChanges() > 200) {
            storeRecommendation(context, ArchitectureRecommendation.builder()
                    .pattern("Modularization")
                    .file(filename)
                    .observation("Large cohesive change in single file")
                    .recommendation("Split into smaller modules or feature packages")
                    .impact("Improves reviewability and parallel development")
                    .build());
        }
    }

    @SuppressWarnings("unchecked")
    private void storeRecommendation(AgentContext context, ArchitectureRecommendation rec) {
        List<ArchitectureRecommendation> list = (List<ArchitectureRecommendation>) context.getMetadata()
                .computeIfAbsent("architectureRecommendations", k -> new ArrayList<ArchitectureRecommendation>());
        list.add(rec);
    }
}
