package com.mergewise.review.security;

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
public class SecurityReviewAnalyzer implements ReviewAnalyzer {

    private static final Pattern SQL_INJECTION = Pattern.compile(
            "(?i)(select|insert|update|delete)\\s.*\\+\\s*[a-zA-Z_]");
    private static final Pattern XSS = Pattern.compile(
            "(?i)innerHTML|document\\.write|dangerouslySetInnerHTML|@ResponseBody.*\\+");
    private static final Pattern HARDCODED_SECRET = Pattern.compile(
            "(?i)(api[_-]?key|secret|password|token|private[_-]?key)\\s*[:=]\\s*[\"'][^\"']{6,}[\"']");
    private static final Pattern NO_AUTH = Pattern.compile("@(Get|Post|Put|Delete)Mapping");
    private static final Pattern PERMIT_ALL = Pattern.compile("permitAll\\(\\)|anonymous\\(\\)");
    private static final Pattern SENSITIVE_LOG = Pattern.compile(
            "(?i)log\\.(info|debug|warn)\\(.*(password|token|ssn|credit)");
    private static final Pattern WEAK_CRYPTO = Pattern.compile("MD5|SHA-1|DES\\b");

    @Override
    public String category() {
        return "SECURITY";
    }

    @Override
    public void analyze(AgentContext context) {
        boolean hasController = false;
        boolean hasSecurityConfig = false;

        for (PRFileChange file : context.getFileChanges()) {
            String name = file.getFilename() != null ? file.getFilename().toLowerCase() : "";
            if (name.contains("controller")) {
                hasController = true;
            }
            if (name.contains("security") || name.contains("websecurity")) {
                hasSecurityConfig = true;
            }
            analyzeFile(context, file);
        }

        if (hasController && !hasSecurityConfig) {
            context.getMetadata().put("authConfigReviewNeeded", true);
        }
    }

    private void analyzeFile(AgentContext context, PRFileChange file) {
        List<AddedLine> lines = PatchLineScanner.scanAddedLines(file);

        for (AddedLine line : lines) {
            String code = line.getCode();

            if (SQL_INJECTION.matcher(code).find()) {
                context.getReviewIssues().add(ReviewIssueFactory.issue(
                        "SECURITY", "CRITICAL", line.getFilename(), line.getPatchLineNumber(),
                        "SQL injection risk",
                        "SQL built via string concatenation with variables",
                        code,
                        "Attackers can execute arbitrary SQL and exfiltrate data",
                        "Use parameterized queries or ORM bindings",
                        "jdbcTemplate.query(\"SELECT * FROM users WHERE id = ?\", id);",
                        95));
            }

            if (XSS.matcher(code).find()) {
                context.getReviewIssues().add(ReviewIssueFactory.issue(
                        "SECURITY", "HIGH", line.getFilename(), line.getPatchLineNumber(),
                        "XSS risk",
                        "Unsanitized output may render user-controlled HTML",
                        code,
                        "Cross-site scripting can compromise user sessions",
                        "Encode output, sanitize input, use CSP headers",
                        "return HtmlUtils.htmlEscape(userInput);",
                        90));
            }

            if (HARDCODED_SECRET.matcher(code).find()) {
                context.getReviewIssues().add(ReviewIssueFactory.issue(
                        "SECURITY", "CRITICAL", line.getFilename(), line.getPatchLineNumber(),
                        "Hardcoded secret detected",
                        "Credential or token embedded in source code",
                        code,
                        "Secrets in source can be leaked via VCS and logs",
                        "Load secrets from environment variables or secret manager",
                        "@Value(\"${api.key}\") private String apiKey;",
                        98));
            }

            if (NO_AUTH.matcher(code).find() && line.getFilename().toLowerCase().contains("controller")
                    && !code.contains("@PreAuthorize") && !code.contains("authenticated")) {
                context.getReviewIssues().add(ReviewIssueFactory.issue(
                        "SECURITY", "MEDIUM", line.getFilename(), line.getPatchLineNumber(),
                        "Authorization review needed",
                        "New endpoint without visible authorization annotation",
                        code,
                        "Unprotected endpoints may expose sensitive operations",
                        "Add @PreAuthorize, security rules, or document public access",
                        "@PreAuthorize(\"hasRole('ADMIN')\")",
                        70));
            }

            if (PERMIT_ALL.matcher(code).find()) {
                context.getReviewIssues().add(ReviewIssueFactory.issue(
                        "SECURITY", "HIGH", line.getFilename(), line.getPatchLineNumber(),
                        "Permissive security rule",
                        "permitAll or anonymous access configured",
                        code,
                        "Overly permissive rules can bypass authentication",
                        "Restrict to required roles and methods explicitly",
                        ".requestMatchers(\"/public/**\").permitAll()",
                        88));
            }

            if (SENSITIVE_LOG.matcher(code).find()) {
                context.getReviewIssues().add(ReviewIssueFactory.issue(
                        "SECURITY", "HIGH", line.getFilename(), line.getPatchLineNumber(),
                        "Sensitive data exposure in logs",
                        "Logging statement may include secrets or PII",
                        code,
                        "Logs are often broadly accessible and retained",
                        "Redact sensitive fields before logging",
                        "log.info(\"User login attempt for id={}\", userId);",
                        87));
            }

            if (WEAK_CRYPTO.matcher(code).find()) {
                context.getReviewIssues().add(ReviewIssueFactory.issue(
                        "SECURITY", "HIGH", line.getFilename(), line.getPatchLineNumber(),
                        "Weak encryption algorithm",
                        "Deprecated cryptographic algorithm in use",
                        code,
                        "Weak crypto can be broken, compromising data confidentiality",
                        "Use AES-256-GCM, bcrypt/argon2 for passwords, SHA-256+ for hashing",
                        "MessageDigest.getInstance(\"SHA-256\")",
                        85));
            }

            if (code.contains("http://") && !code.contains("localhost")) {
                context.getReviewIssues().add(ReviewIssueFactory.issue(
                        "SECURITY", "MEDIUM", line.getFilename(), line.getPatchLineNumber(),
                        "Insecure HTTP endpoint",
                        "Non-TLS URL configured for external communication",
                        code,
                        "Data in transit can be intercepted",
                        "Use HTTPS endpoints and enforce TLS",
                        "https://api.example.com/v1/resource",
                        80));
            }
        }
    }
}
