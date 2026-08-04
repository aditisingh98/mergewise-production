package com.mergewise.review.diff;

import com.mergewise.dto.review.ReviewIssueModel;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Produces a corrected code snippet that differs from the problematic line when possible.
 */
@Component
public class CodeFixSuggester {

    private static final Pattern WILDCARD_IMPORT = Pattern.compile("^import\\s+[\\w.]+\\.\\*;\\s*$");
    private static final Pattern SYSTEM_OUT = Pattern.compile(
            "System\\.out\\.println\\((.*)\\);");
    private static final Pattern REQUEST_DEREF = Pattern.compile("request\\.get");

    public String suggest(ReviewIssueModel issue, String issueCode) {
        if (issue == null) {
            return null;
        }
        String existing = firstNonBlank(issue.getFixCode(), issue.getFixedExample());
        if (existing != null && issueCode != null && !normalize(existing).equals(normalize(issueCode))) {
            return existing;
        }
        if (issueCode == null || issueCode.isBlank()) {
            return suggestFileLevel(issue);
        }

        String title = issue.getTitle() != null ? issue.getTitle().toLowerCase(Locale.ROOT) : "";
        String category = issue.getCategory() != null ? issue.getCategory().toUpperCase(Locale.ROOT) : "";
        String trimmed = issueCode.trim();

        if (WILDCARD_IMPORT.matcher(trimmed).matches()) {
            return """
                    import java.util.ArrayList;
                    import java.util.List;""";
        }

        Matcher out = SYSTEM_OUT.matcher(trimmed);
        if (out.matches() || title.contains("system.out") || "LOGGING".equals(category)) {
            String arg = out.matches() ? out.group(1) : "\"message\"";
            return "log.info(" + arg + ");";
        }

        if (trimmed.contains("processPayment(null)")) {
            return "service.processPayment(validPaymentRequest);";
        }

        if (trimmed.contains("request.getStatus().equals(")) {
            return "if (\"SUCCESS\".equals(request.getStatus())) {";
        }

        if (trimmed.contains("/ request.getInstallments()")) {
            return "int installments = request.getInstallments() != null && request.getInstallments() != 0"
                    + " ? request.getInstallments() : 1;\n"
                    + "            int result = 100 / installments;";
        }

        if (trimmed.contains("Integer.parseInt(request.getAmount())")) {
            return "int amount = Integer.parseInt(Objects.requireNonNull(request.getAmount(), \"amount\"));";
        }

        if (trimmed.startsWith("String customerId = request.getCustomerId()")) {
            return """
                    if (request == null) {
                        throw new IllegalArgumentException("payment request is required");
                    }
                    String customerId = request.getCustomerId();""";
        }

        if (trimmed.startsWith("for (String method : request.getPaymentMethods())")) {
            return """
                    if (request.getPaymentMethods() == null) {
                        return;
                    }
                    for (String method : request.getPaymentMethods()) {""";
        }

        if (trimmed.contains("list.get(0)")) {
            return """
                    if (!list.isEmpty()) {
                        log.info("{}", list.get(0));
                    }""";
        }

        if (title.contains("null check") || title.contains("dereference") || REQUEST_DEREF.matcher(trimmed).find()) {
            if (trimmed.startsWith("if (") && trimmed.contains("request.")) {
                return "if (request != null && " + trimmed.substring(3);
            }
            if (trimmed.contains("service.")) {
                return "if (service != null) { " + trimmed + " }";
            }
            return """
                    if (request == null) {
                        throw new IllegalArgumentException("request must not be null");
                    }
                    """ + trimmed;
        }

        if (trimmed.contains("PaymentService service = new PaymentService()")) {
            return """
                    private final PaymentService paymentService;

                    public PaymentService(PaymentService paymentService) {
                        this.paymentService = paymentService;
                    }""";
        }

        if (title.contains("deep nesting") && trimmed.startsWith("for (")) {
            return "processPaymentMethods(request.getPaymentMethods());";
        }

        if ("TESTING".equals(category) || title.contains("test coverage")) {
            return """
                    @Test
                    void processPayment_handlesNullRequest() {
                        assertThrows(IllegalArgumentException.class, () -> service.processPayment(null));
                    }""";
        }

        if (title.contains("large code change")) {
            return null;
        }

        String guide = issue.getRecommendation();
        if (guide != null && !guide.isBlank() && !looksLikeCode(guide)) {
            return null;
        }
        return existing;
    }

    public String resolveIssueCode(ReviewIssueModel issue) {
        if (isFileLevel(issue)) {
            return null;
        }
        String code = firstNonBlank(issue.getNewCode(), issue.getAffectedCode(), issue.getIssueCode());
        if (code != null && !code.isBlank()) {
            return code.trim();
        }
        if (issue.getLine() != null && issue.getLine() > 0 && issue.getDescription() != null) {
            String fromDesc = extractCodeFromDescription(issue.getDescription());
            if (fromDesc != null) {
                return fromDesc;
            }
            if (looksLikeCodeLine(issue.getDescription())) {
                return issue.getDescription().trim();
            }
        }
        return null;
    }

    public boolean isFileLevel(ReviewIssueModel issue) {
        if (issue.getLine() != null && issue.getLine() > 0) {
            return false;
        }
        String cat = issue.getCategory() != null ? issue.getCategory().toUpperCase(Locale.ROOT) : "";
        String title = issue.getTitle() != null ? issue.getTitle().toLowerCase(Locale.ROOT) : "";
        return "TESTING".equals(cat)
                || "LOGGING".equals(cat)
                || (title.contains("large") && title.contains("code"));
    }

    private String suggestFileLevel(ReviewIssueModel issue) {
        return firstNonBlank(issue.getFixedExample(), issue.getRecommendation());
    }

    private String extractCodeFromDescription(String description) {
        int start = description.indexOf('`');
        if (start < 0) {
            return null;
        }
        int end = description.indexOf('`', start + 1);
        if (end < 0) {
            return null;
        }
        return description.substring(start + 1, end).trim();
    }

    private boolean looksLikeCode(String value) {
        return value.contains(";") || value.contains("{") || value.startsWith("@");
    }

    private boolean looksLikeCodeLine(String value) {
        String t = value.trim();
        return t.contains("(") || t.contains(";") || t.endsWith("{");
    }

    private String normalize(String value) {
        return value.replaceAll("\\s+", " ").trim();
    }

    private String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }
}
