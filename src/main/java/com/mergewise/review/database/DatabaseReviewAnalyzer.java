package com.mergewise.review.database;

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
public class DatabaseReviewAnalyzer implements ReviewAnalyzer {

    private static final Pattern SELECT_STAR = Pattern.compile("(?i)SELECT\\s+\\*\\s+FROM");
    private static final Pattern MISSING_WHERE = Pattern.compile("(?i)(UPDATE|DELETE)\\s+\\w+\\s*(;|$)");
    private static final Pattern NO_TRANSACTION = Pattern.compile("@Transactional");
    private static final Pattern MIGRATION = Pattern.compile("(?i)(flyway|liquibase|migration|ALTER TABLE|DROP TABLE)");
    private static final Pattern INDEX_HINT = Pattern.compile("(?i)WHERE\\s+\\w+\\s*=");

    @Override
    public String category() {
        return "DATABASE";
    }

    @Override
    public void analyze(AgentContext context) {
        for (PRFileChange file : context.getFileChanges()) {
            if (!PatchLineScanner.isJavaFile(file) && !PatchLineScanner.isSqlFile(file)) {
                continue;
            }
            analyzeFile(context, file);
        }
    }

    private void analyzeFile(AgentContext context, PRFileChange file) {
        List<AddedLine> lines = PatchLineScanner.scanAddedLines(file);
        boolean hasWrite = false;
        boolean hasTransactional = false;

        for (AddedLine line : lines) {
            String code = line.getCode();

            if (SELECT_STAR.matcher(code).find()) {
                context.getReviewIssues().add(ReviewIssueFactory.issue(
                        "DATABASE", "MEDIUM", line.getFilename(), line.getPatchLineNumber(),
                        "Query optimization opportunity",
                        "SELECT * fetches unnecessary columns",
                        code,
                        "Extra columns increase I/O and memory usage",
                        "Select only required columns",
                        "SELECT id, name, status FROM orders WHERE id = ?",
                        75));
            }

            if (MISSING_WHERE.matcher(code).find()) {
                context.getReviewIssues().add(ReviewIssueFactory.issue(
                        "DATABASE", "HIGH", line.getFilename(), line.getPatchLineNumber(),
                        "Unscoped data modification",
                        "UPDATE/DELETE without WHERE clause",
                        code,
                        "Can modify or delete entire table causing data loss",
                        "Add WHERE clause with primary key or scoped filter",
                        "DELETE FROM sessions WHERE expired_at < NOW()",
                        94));
            }

            if (INDEX_HINT.matcher(code).find() && !code.toLowerCase().contains("index")) {
                context.getReviewIssues().add(ReviewIssueFactory.issue(
                        "DATABASE", "LOW", line.getFilename(), line.getPatchLineNumber(),
                        "Missing index suggestion",
                        "Filtered query without index hint in migration",
                        code,
                        "Full table scans slow queries at scale",
                        "Add index on frequently filtered columns",
                        "CREATE INDEX idx_orders_customer_id ON orders(customer_id);",
                        68));
            }

            if (MIGRATION.matcher(code).find()) {
                context.getReviewIssues().add(ReviewIssueFactory.issue(
                        "DATABASE", "MEDIUM", line.getFilename(), line.getPatchLineNumber(),
                        "Migration impact",
                        "Schema migration detected",
                        code,
                        "Migrations can lock tables and affect deployment rollout",
                        "Use backward-compatible migrations, test rollback, deploy off-peak",
                        "-- Step 1: add nullable column; Step 2: backfill; Step 3: enforce NOT NULL",
                        80));
            }

            if (code.contains("save(") || code.contains("delete(") || code.contains("update(")) {
                hasWrite = true;
            }
            if (NO_TRANSACTION.matcher(code).find()) {
                hasTransactional = true;
            }
        }

        if (hasWrite && PatchLineScanner.isJavaFile(file) && !hasTransactional) {
            context.getReviewIssues().add(ReviewIssueFactory.issue(
                    "DATABASE", "MEDIUM", file.getFilename(), 0,
                    "Transaction boundary concern",
                    "Write operations without visible @Transactional",
                    "Multiple write operations in service layer",
                    "Partial writes can cause data inconsistency",
                    "Wrap multi-step writes in a single transactional boundary",
                    "@Transactional public void transferFunds() { }",
                    78));
        }
    }
}
