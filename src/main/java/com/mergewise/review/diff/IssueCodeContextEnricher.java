package com.mergewise.review.diff;

import com.mergewise.dto.PRFileChange;
import com.mergewise.dto.review.CodeComparison;
import com.mergewise.dto.review.DiffLine;
import com.mergewise.dto.review.ReviewIssueModel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Attaches diff-backed code context, old/new comparison, and developer guidance to review issues.
 */
@Component
public class IssueCodeContextEnricher {

    private static final Pattern ISSUE_PREFIX = Pattern.compile(
            "^ISSUE:\\s*\\[(?:critical|high|medium|low)]\\s*",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern FILE_LINE_IN_DESC = Pattern.compile(
            "^[^:]+:(\\d+)\\s*-\\s*");

    private final PatchDiffParser patchDiffParser;

    public IssueCodeContextEnricher(PatchDiffParser patchDiffParser) {
        this.patchDiffParser = patchDiffParser;
    }

    public List<ReviewIssueModel> enrich(List<PRFileChange> fileChanges, List<ReviewIssueModel> issues) {
        if (issues == null || issues.isEmpty()) {
            return issues != null ? issues : List.of();
        }
        Map<String, List<DiffLine>> diffByFile = indexDiffs(fileChanges);
        Map<String, PRFileChange> changeByFile = indexChanges(fileChanges);

        List<ReviewIssueModel> enriched = new ArrayList<>();
        for (ReviewIssueModel issue : issues) {
            enriched.add(enrichOne(issue, diffByFile, changeByFile));
        }
        return enriched;
    }

    public List<DiffLine> annotateDiffWithIssues(List<DiffLine> diffLines, List<ReviewIssueModel> fileIssues) {
        if (diffLines == null || diffLines.isEmpty() || fileIssues == null || fileIssues.isEmpty()) {
            return diffLines != null ? diffLines : List.of();
        }
        List<DiffLine> annotated = new ArrayList<>();
        for (DiffLine line : diffLines) {
            DiffLine copy = DiffLine.builder()
                    .type(line.getType())
                    .content(line.getContent())
                    .oldLineNumber(line.getOldLineNumber())
                    .newLineNumber(line.getNewLineNumber())
                    .issueIds(new ArrayList<>())
                    .highlighted(false)
                    .build();

            for (ReviewIssueModel issue : fileIssues) {
                if (matchesLine(issue, line)) {
                    copy.getIssueIds().add(issue.getId());
                }
            }
            copy.setHighlighted(!copy.getIssueIds().isEmpty());
            annotated.add(copy);
        }
        return annotated;
    }

    private ReviewIssueModel enrichOne(
            ReviewIssueModel issue,
            Map<String, List<DiffLine>> diffByFile,
            Map<String, PRFileChange> changeByFile) {

        ReviewIssueModel.ReviewIssueModelBuilder builder = issue.toBuilder();
        polishNarrative(builder, issue);

        String file = issue.getFile();
        if (file == null || file.isBlank() || "—".equals(file)) {
            ReviewIssueModel built = builder.build();
            return built.toBuilder()
                    .developmentGuidance(defaultGuidance(built))
                    .build();
        }

        List<DiffLine> diff = diffByFile.getOrDefault(file, List.of());
        int targetLine = resolveLineNumber(issue, diff);

        LineContext ctx = locateContext(diff, targetLine, issue);
        if (ctx != null) {
            builder.line(ctx.effectiveLine() > 0 ? ctx.effectiveLine() : issue.getLine());
            builder.newCode(firstNonBlank(issue.getNewCode(), issue.getFixedExample(), ctx.newCode(), ctx.addedOrContext()));
            builder.affectedCode(firstNonBlank(issue.getAffectedCode(), issue.getFixedExample(), ctx.newCode(), ctx.addedOrContext()));
            builder.oldCode(firstNonBlank(issue.getOldCode(), ctx.oldCode()));
            builder.codeSnippet(ctx.snippet());
            builder.codeComparison(CodeComparison.builder()
                    .before(ctx.beforeBlock())
                    .after(ctx.afterBlock())
                    .contextSnippet(ctx.snippet())
                    .build());
            ReviewIssueModel partial = builder.build();
            if (partial.getFixedExample() == null || partial.getFixedExample().isBlank()) {
                builder.fixedExample(suggestFixFromContext(issue, ctx));
            }
        } else {
            PRFileChange change = changeByFile.get(file);
            builder.affectedCode(firstNonBlank(issue.getAffectedCode(), guessFromAddedLines(change, issue)));
        }

        ReviewIssueModel built = builder.build();
        if (built.getDevelopmentGuidance() == null || built.getDevelopmentGuidance().isBlank()) {
            built = built.toBuilder()
                    .developmentGuidance(buildDevelopmentGuidance(built))
                    .build();
        }
        return applyIssueFixCodes(built);
    }

    private ReviewIssueModel applyIssueFixCodes(ReviewIssueModel built) {
        String issueCode = firstNonBlank(
                built.getIssueCode(),
                built.getNewCode(),
                built.getAffectedCode(),
                built.getOldCode());
        String fixCode = firstNonBlank(built.getFixCode(), built.getFixedExample(), inferFixCode(built));
        return built.toBuilder()
                .issueCode(issueCode)
                .fixCode(fixCode)
                .build();
    }

    private String inferFixCode(ReviewIssueModel issue) {
        if (issue.getIssueCode() == null && issue.getNewCode() == null) {
            return null;
        }
        String line = firstNonBlank(issue.getIssueCode(), issue.getNewCode(), issue.getAffectedCode());
        if (line == null) {
            return null;
        }
        String lower = line.toLowerCase(Locale.ROOT);
        if (lower.contains("system.out") || lower.contains("system.err")) {
            return line.replaceAll("System\\.(out|err)\\.print\\w*", "log.info");
        }
        if (line.matches(".*\\b\\w+\\s*=\\s*null\\s*;.*")) {
            return line.replace("= null", "= Objects.requireNonNull(value, \"value\")");
        }
        return null;
    }

    private void polishNarrative(ReviewIssueModel.ReviewIssueModelBuilder builder, ReviewIssueModel issue) {
        String description = cleanDescription(issue.getDescription(), issue.getTitle());
        if (description == null || description.isBlank()) {
            description = issue.getTitle();
        }
        builder.description(description);

        if (issue.getRootCause() == null || issue.getRootCause().isBlank()
                || issue.getRootCause().equals(issue.getTitle())) {
            builder.rootCause(description);
        } else {
            String oldFromRoot = extractBeforeLine(issue.getRootCause());
            if (oldFromRoot != null) {
                builder.oldCode(firstNonBlank(issue.getOldCode(), oldFromRoot));
            }
        }

        if (issue.getProductionImpact() == null || issue.getProductionImpact().isBlank()) {
            builder.productionImpact(defaultProductionImpact(issue.getCategory(), issue.getSeverity()));
        }

        if (issue.getRecommendation() == null || issue.getRecommendation().isBlank()) {
            builder.recommendation("Refactor the highlighted code to remove the risk described above and add tests that cover this path.");
        }
    }

    private Map<String, List<DiffLine>> indexDiffs(List<PRFileChange> fileChanges) {
        Map<String, List<DiffLine>> map = new LinkedHashMap<>();
        if (fileChanges == null) {
            return map;
        }
        for (PRFileChange change : fileChanges) {
            if (change.getFilename() != null) {
                map.put(change.getFilename(), patchDiffParser.parse(change.getPatch()));
            }
        }
        return map;
    }

    private Map<String, PRFileChange> indexChanges(List<PRFileChange> fileChanges) {
        Map<String, PRFileChange> map = new LinkedHashMap<>();
        if (fileChanges == null) {
            return map;
        }
        for (PRFileChange change : fileChanges) {
            if (change.getFilename() != null) {
                map.put(change.getFilename(), change);
            }
        }
        return map;
    }

    private int resolveLineNumber(ReviewIssueModel issue, List<DiffLine> diff) {
        if (issue.getLine() != null && issue.getLine() > 0) {
            return issue.getLine();
        }
        String desc = issue.getDescription();
        if (desc != null) {
            Matcher m = FILE_LINE_IN_DESC.matcher(desc);
            if (m.find()) {
                return Integer.parseInt(m.group(1));
            }
        }
        String probe = firstNonBlank(issue.getAffectedCode(), issue.getNewCode());
        if (probe != null) {
            for (DiffLine line : diff) {
                if (("ADDED".equals(line.getType()) || "CONTEXT".equals(line.getType()))
                        && line.getContent() != null
                        && line.getContent().contains(probe.trim())) {
                    return line.getNewLineNumber() != null ? line.getNewLineNumber() : 0;
                }
            }
        }
        return guessLineFromTitle(issue, diff);
    }

    private int guessLineFromTitle(ReviewIssueModel issue, List<DiffLine> diff) {
        String title = issue.getTitle() != null ? issue.getTitle().toLowerCase(Locale.ROOT) : "";
        for (DiffLine line : diff) {
            if (!"ADDED".equals(line.getType()) || line.getContent() == null) {
                continue;
            }
            String content = line.getContent().toLowerCase(Locale.ROOT);
            if (title.contains("null") && content.contains("null")) {
                return line.getNewLineNumber() != null ? line.getNewLineNumber() : 0;
            }
            if (title.contains("sql") && (content.contains("select") || content.contains("insert"))) {
                return line.getNewLineNumber() != null ? line.getNewLineNumber() : 0;
            }
            if (title.contains("secret") && (content.contains("password") || content.contains("token"))) {
                return line.getNewLineNumber() != null ? line.getNewLineNumber() : 0;
            }
        }
        return 0;
    }

    private LineContext locateContext(List<DiffLine> diff, int targetLine, ReviewIssueModel issue) {
        if (diff.isEmpty()) {
            return null;
        }

        int index = -1;
        if (targetLine > 0) {
            for (int i = 0; i < diff.size(); i++) {
                DiffLine line = diff.get(i);
                Integer newLn = line.getNewLineNumber();
                Integer oldLn = line.getOldLineNumber();
                if ((newLn != null && targetLine == newLn) || (oldLn != null && targetLine == oldLn)) {
                    index = i;
                    break;
                }
            }
        }

        if (index < 0) {
            index = findByContent(diff, issue);
        }
        if (index < 0) {
            return null;
        }

        DiffLine focus = diff.get(index);
        String oldCode = null;
        String newCode = null;
        if ("REMOVED".equals(focus.getType())) {
            oldCode = focus.getContent();
            if (index + 1 < diff.size() && "ADDED".equals(diff.get(index + 1).getType())) {
                newCode = diff.get(index + 1).getContent();
            }
        } else if ("ADDED".equals(focus.getType())) {
            newCode = focus.getContent();
            if (index > 0 && "REMOVED".equals(diff.get(index - 1).getType())) {
                oldCode = diff.get(index - 1).getContent();
            }
        } else {
            newCode = focus.getContent();
        }

        int effectiveLine = focus.getNewLineNumber() != null ? focus.getNewLineNumber()
                : (focus.getOldLineNumber() != null ? focus.getOldLineNumber() : targetLine);

        int from = Math.max(0, index - 2);
        int to = Math.min(diff.size(), index + 3);
        StringBuilder snippet = new StringBuilder();
        List<String> before = new ArrayList<>();
        List<String> after = new ArrayList<>();
        for (int i = from; i < to; i++) {
            DiffLine line = diff.get(i);
            if ("HUNK".equals(line.getType())) {
                continue;
            }
            String prefix = switch (line.getType()) {
                case "REMOVED" -> "- ";
                case "ADDED" -> "+ ";
                default -> "  ";
            };
            snippet.append(prefix).append(line.getContent()).append('\n');
            if ("REMOVED".equals(line.getType())) {
                before.add(line.getContent());
            } else if ("ADDED".equals(line.getType())) {
                after.add(line.getContent());
            }
        }

        return new LineContext(
                effectiveLine,
                oldCode,
                newCode,
                newCode != null ? newCode : focus.getContent(),
                snippet.toString().trim(),
                before,
                after);
    }

    private int findByContent(List<DiffLine> diff, ReviewIssueModel issue) {
        String needle = firstNonBlank(issue.getAffectedCode(), issue.getNewCode());
        if (needle == null) {
            return -1;
        }
        String trimmed = needle.trim();
        for (int i = 0; i < diff.size(); i++) {
            DiffLine line = diff.get(i);
            if (line.getContent() != null && line.getContent().contains(trimmed)) {
                return i;
            }
        }
        return -1;
    }

    private boolean matchesLine(ReviewIssueModel issue, DiffLine line) {
        if (issue.getLine() != null && issue.getLine() > 0) {
            Integer newLn = line.getNewLineNumber();
            Integer oldLn = line.getOldLineNumber();
            if ((newLn != null && issue.getLine().equals(newLn))
                    || (oldLn != null && issue.getLine().equals(oldLn))) {
                return true;
            }
        }
        String affected = firstNonBlank(issue.getAffectedCode(), issue.getNewCode());
        if (affected != null && line.getContent() != null) {
            return line.getContent().contains(affected.trim());
        }
        return false;
    }

    private String guessFromAddedLines(PRFileChange change, ReviewIssueModel issue) {
        if (change == null || change.getAddedLines() == null || change.getAddedLines().isEmpty()) {
            return null;
        }
        String title = issue.getTitle() != null ? issue.getTitle().toLowerCase(Locale.ROOT) : "";
        for (String line : change.getAddedLines()) {
            String lower = line.toLowerCase(Locale.ROOT);
            if (title.contains("null") && lower.contains("null")) {
                return line;
            }
        }
        return change.getAddedLines().get(0);
    }

    private String cleanDescription(String description, String title) {
        if (description == null) {
            return title;
        }
        String cleaned = ISSUE_PREFIX.matcher(description).replaceFirst("").trim();
        if (cleaned.contains(" - ")) {
            int dash = cleaned.indexOf(" - ");
            String maybeFile = cleaned.substring(0, dash).trim();
            if (maybeFile.contains("/") || maybeFile.endsWith(".java") || maybeFile.contains(".")) {
                cleaned = cleaned.substring(dash + 3).trim();
            }
        }
        return cleaned.isBlank() ? title : cleaned;
    }

    private String extractBeforeLine(String rootCause) {
        if (rootCause == null) {
            return null;
        }
        int idx = rootCause.indexOf("Before:");
        if (idx < 0) {
            return null;
        }
        return rootCause.substring(idx + "Before:".length()).trim().split("\\R")[0].trim();
    }

    private String buildDevelopmentGuidance(ReviewIssueModel built) {
        if (built.getDevelopmentGuidance() != null && !built.getDevelopmentGuidance().isBlank()) {
            return built.getDevelopmentGuidance();
        }
        StringBuilder sb = new StringBuilder();
        if (built.getRecommendation() != null && !built.getRecommendation().isBlank()) {
            sb.append(built.getRecommendation().trim());
        }
        if (built.getNewCode() != null && !built.getNewCode().isBlank()) {
            if (!sb.isEmpty()) {
                sb.append("\n\n");
            }
            sb.append("Problematic code in this PR:\n").append(built.getNewCode().trim());
        }
        if (built.getOldCode() != null && !built.getOldCode().isBlank()) {
            sb.append("\n\nPrevious code:\n").append(built.getOldCode().trim());
        }
        if (built.getFixedExample() != null && !built.getFixedExample().isBlank()) {
            sb.append("\n\nSuggested approach:\n").append(built.getFixedExample().trim());
        }
        if (sb.isEmpty()) {
            return defaultGuidance(built);
        }
        return sb.toString();
    }

    private String defaultGuidance(ReviewIssueModel issue) {
        String category = issue.getCategory() != null ? issue.getCategory().toUpperCase(Locale.ROOT) : "GENERAL";
        return switch (category) {
            case "SECURITY" -> "Eliminate the unsafe pattern, add validation at trust boundaries, and add a regression test that proves the exploit path is closed.";
            case "NPE", "RUNTIME_RISK" -> "Add null-safety (checks, Optional, or annotations), fail fast with clear errors, and cover the branch in unit tests.";
            case "PERFORMANCE" -> "Measure the hot path, reduce allocations or query count, and add a benchmark or load test if this is user-facing.";
            case "TESTING" -> "Add automated tests that exercise the changed behavior and edge cases introduced by this PR.";
            default -> "Refactor the changed lines to follow project conventions, then verify with tests and a focused code review.";
        };
    }

    private String defaultProductionImpact(String category, String severity) {
        String sev = severity != null ? severity.toUpperCase(Locale.ROOT) : "MEDIUM";
        if ("CRITICAL".equals(sev) || "HIGH".equals(sev)) {
            return "May cause outages, data loss, or security exposure if merged without a fix.";
        }
        if ("SECURITY".equalsIgnoreCase(category)) {
            return "Security weakness in changed code; assess exposure before release.";
        }
        return "May degrade reliability or maintainability in production if left unaddressed.";
    }

    private String suggestFixFromContext(ReviewIssueModel issue, LineContext ctx) {
        if (ctx.newCode() == null) {
            return null;
        }
        String lower = ctx.newCode().toLowerCase(Locale.ROOT);
        if (lower.contains("= null")) {
            return "Use Optional, a non-null default, or validate before dereferencing instead of assigning null.";
        }
        if (lower.contains("system.out")) {
            return "Replace with SLF4J: private static final Logger log = LoggerFactory.getLogger(...); log.info(...);";
        }
        return issue.getRecommendation();
    }

    private String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }

    private record LineContext(
            int effectiveLine,
            String oldCode,
            String newCode,
            String addedOrContext,
            String snippet,
            List<String> beforeBlock,
            List<String> afterBlock) {
    }
}
