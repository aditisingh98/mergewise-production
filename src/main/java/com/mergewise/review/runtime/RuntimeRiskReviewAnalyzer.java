package com.mergewise.review.runtime;

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
public class RuntimeRiskReviewAnalyzer implements ReviewAnalyzer {

    private static final Pattern ARRAY_ACCESS = Pattern.compile("\\w+\\[\\s*\\w+\\s*\\]");
    private static final Pattern WHILE_TRUE = Pattern.compile("while\\s*\\(\\s*true\\s*\\)");
    private static final Pattern RESOURCE_OPEN = Pattern.compile("new\\s+(FileInputStream|FileOutputStream|BufferedReader|Connection|Socket)");
    private static final Pattern ASYNC = Pattern.compile("CompletableFuture|@Async|ExecutorService|submit\\(");
    private static final Pattern SYNC = Pattern.compile("synchronized\\s*\\(|ReentrantLock|volatile\\s+");
    private static final Pattern EMPTY_CATCH = Pattern.compile("catch\\s*\\([^)]+\\)\\s*\\{\\s*\\}");

    @Override
    public String category() {
        return "RUNTIME_RISK";
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

        for (AddedLine line : lines) {
            String code = line.getCode();

            if (code.contains("=null") || code.contains("= null")) {
                context.getReviewIssues().add(ReviewIssueFactory.issue(
                        "RUNTIME_RISK", "HIGH", line.getFilename(), line.getPatchLineNumber(),
                        "Possible null assignment",
                        "Variable explicitly assigned null",
                        code,
                        "May cause NullPointerException during runtime",
                        "Initialize with safe default, use Optional, or validate before use",
                        "String value = Objects.requireNonNullElse(input, \"\");",
                        90));
            }

            if (code.contains(".equalsIgnoreCase(") && !code.contains("!= null") && !code.contains("Objects.equals")) {
                context.getReviewIssues().add(ReviewIssueFactory.issue(
                        "RUNTIME_RISK", "CRITICAL", line.getFilename(), line.getPatchLineNumber(),
                        "Unsafe null dereference",
                        "equalsIgnoreCase called without null validation",
                        code,
                        "Can crash production API with NullPointerException",
                        "Use Objects.equals or null-safe comparison",
                        "if (Objects.equals(value, \"test\")) { }",
                        97));
            }

            if (ARRAY_ACCESS.matcher(code).find() && !code.contains(".length")) {
                context.getReviewIssues().add(ReviewIssueFactory.issue(
                        "RUNTIME_RISK", "HIGH", line.getFilename(), line.getPatchLineNumber(),
                        "Unsafe array access",
                        "Array index used without visible bounds check",
                        code,
                        "ArrayIndexOutOfBoundsException can crash request handling",
                        "Validate index against array length before access",
                        "if (index >= 0 && index < array.length) { }",
                        84));
            }

            if (WHILE_TRUE.matcher(code).find() && !code.contains("break")) {
                context.getReviewIssues().add(ReviewIssueFactory.issue(
                        "RUNTIME_RISK", "HIGH", line.getFilename(), line.getPatchLineNumber(),
                        "Potential infinite loop",
                        "while(true) without visible break condition in same line",
                        code,
                        "Infinite loops can hang threads and exhaust resources",
                        "Add explicit exit conditions or use bounded iteration",
                        "while (running) { if (shouldStop()) break; }",
                        88));
            }

            if (RESOURCE_OPEN.matcher(code).find() && !code.contains("try-with-resources") && !code.contains("try (")) {
                context.getReviewIssues().add(ReviewIssueFactory.issue(
                        "RUNTIME_RISK", "MEDIUM", line.getFilename(), line.getPatchLineNumber(),
                        "Possible resource leak",
                        "Resource opened without try-with-resources",
                        code,
                        "Unclosed streams or connections can leak memory and file handles",
                        "Use try-with-resources or ensure close in finally block",
                        "try (var stream = new FileInputStream(path)) { }",
                        86));
            }

            if (ASYNC.matcher(code).find() && !code.contains("exceptionally") && !code.contains("handle(")) {
                context.getReviewIssues().add(ReviewIssueFactory.issue(
                        "RUNTIME_RISK", "MEDIUM", line.getFilename(), line.getPatchLineNumber(),
                        "Async handling gap",
                        "Async execution without visible error handling",
                        code,
                        "Unhandled async failures may fail silently in production",
                        "Add exceptionally/handle callbacks and timeout policies",
                        "future.exceptionally(ex -> { log.error(\"failed\", ex); return null; });",
                        75));
            }

            if (SYNC.matcher(code).find()) {
                context.getReviewIssues().add(ReviewIssueFactory.issue(
                        "RUNTIME_RISK", "MEDIUM", line.getFilename(), line.getPatchLineNumber(),
                        "Concurrency concern",
                        "Synchronization primitive introduced",
                        code,
                        "Incorrect locking can cause deadlocks or race conditions",
                        "Review lock ordering, prefer higher-level concurrency utilities",
                        "private final Object lock = new Object();",
                        70));
            }

            if (EMPTY_CATCH.matcher(code).find()) {
                context.getReviewIssues().add(ReviewIssueFactory.issue(
                        "RUNTIME_RISK", "MEDIUM", line.getFilename(), line.getPatchLineNumber(),
                        "Exception handling gap",
                        "Empty catch block swallows exceptions",
                        code,
                        "Silent failures hide production defects",
                        "Log and rethrow or map to domain-specific errors",
                        "catch (IOException ex) { log.error(\"I/O failed\", ex); throw ex; }",
                        92));
            }
        }
    }
}
