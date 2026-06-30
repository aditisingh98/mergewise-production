package com.mergewise.review.performance;

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
public class PerformanceReviewAnalyzer implements ReviewAnalyzer {

    private static final Pattern N_PLUS_ONE = Pattern.compile(
            "(?i)(for|while)\\s*\\([^)]*\\)\\s*\\{[^}]*(findBy|getOne|query|execute|fetch)");
    private static final Pattern NESTED_LOOP = Pattern.compile("for\\s*\\([^)]*\\)[^{]*for\\s*\\(");
    private static final Pattern HTTP_CALL = Pattern.compile("WebClient|RestTemplate|HttpClient|fetch\\(");
    private static final Pattern NO_CACHE = Pattern.compile("findAll\\(|getAll\\(|list\\(");
    private static final Pattern NO_PAGINATION = Pattern.compile("findAll\\(\\)|SELECT \\* FROM");

    @Override
    public String category() {
        return "PERFORMANCE";
    }

    @Override
    public void analyze(AgentContext context) {
        StringBuilder addedCode = new StringBuilder();

        for (PRFileChange file : context.getFileChanges()) {
            List<AddedLine> lines = PatchLineScanner.scanAddedLines(file);
            for (AddedLine line : lines) {
                addedCode.append(line.getCode()).append('\n');
                checkLine(context, line);
            }
        }

        if (NESTED_LOOP.matcher(addedCode).find()) {
            context.getMetadata().put("performanceNestedLoops", true);
        }
    }

    private void checkLine(AgentContext context, AddedLine line) {
        String code = line.getCode();

        if (N_PLUS_ONE.matcher(code).find()) {
            context.getReviewIssues().add(ReviewIssueFactory.issue(
                    "PERFORMANCE", "HIGH", line.getFilename(), line.getPatchLineNumber(),
                    "Possible N+1 query pattern",
                    "Database or API call inside iteration",
                    code,
                    "N+1 patterns degrade latency under load",
                    "Batch queries, use JOIN FETCH, or eager loading strategies",
                    "List<Order> orders = orderRepository.findByCustomerIdIn(ids);",
                    83));
        }

        if (NESTED_LOOP.matcher(code).find()) {
            context.getReviewIssues().add(ReviewIssueFactory.issue(
                    "PERFORMANCE", "MEDIUM", line.getFilename(), line.getPatchLineNumber(),
                    "Nested loop detected",
                    "O(n^2) iteration pattern in added code",
                    code,
                    "Nested loops may cause performance bottlenecks on large datasets",
                    "Use maps/sets for lookups or stream-based aggregation",
                    "Map<String, Item> index = items.stream().collect(toMap(Item::getId, i -> i));",
                    78));
        }

        if (HTTP_CALL.matcher(code).find()) {
            context.getReviewIssues().add(ReviewIssueFactory.issue(
                    "PERFORMANCE", "MEDIUM", line.getFilename(), line.getPatchLineNumber(),
                    "External API call in code path",
                    "HTTP client usage may add latency",
                    code,
                    "Multiple sequential API calls increase response time",
                    "Parallelize independent calls, add timeouts and circuit breakers",
                    "CompletableFuture.allOf(callA, callB).join();",
                    72));
        }

        if (NO_CACHE.matcher(code).find() && code.contains("Repository")) {
            context.getReviewIssues().add(ReviewIssueFactory.issue(
                    "PERFORMANCE", "LOW", line.getFilename(), line.getPatchLineNumber(),
                    "Missing caching opportunity",
                    "Bulk repository read without caching hint",
                    code,
                    "Repeated reads increase database load",
                    "Add @Cacheable for stable reference data or application-level cache",
                    "@Cacheable(\"products\") public List<Product> findAll() { }",
                    65));
        }

        if (NO_PAGINATION.matcher(code).find()) {
            context.getReviewIssues().add(ReviewIssueFactory.issue(
                    "PERFORMANCE", "MEDIUM", line.getFilename(), line.getPatchLineNumber(),
                    "Pagination suggestion",
                    "Unbounded data fetch detected",
                    code,
                    "Loading all records can cause memory pressure and slow responses",
                    "Use Pageable, LIMIT/OFFSET, or cursor-based pagination",
                    "Page<Order> findAll(Pageable pageable);",
                    80));
        }

        if (code.contains("stream()") && code.contains("collect") && code.contains("forEach")) {
            context.getReviewIssues().add(ReviewIssueFactory.issue(
                    "PERFORMANCE", "LOW", line.getFilename(), line.getPatchLineNumber(),
                    "Redundant processing",
                    "Multiple stream passes over same collection",
                    code,
                    "Extra iterations waste CPU cycles",
                    "Combine stream operations into a single pass",
                    "items.stream().filter(this::valid).map(this::toDto).toList();",
                    62));
        }
    }
}
