package com.mergewise.review.diff;

import com.mergewise.dto.review.DiffLine;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PatchDiffParser {

    private static final Pattern HUNK = Pattern.compile(
            "^@@\\s+-([0-9]+)(?:,([0-9]+))?\\s+\\+([0-9]+)(?:,([0-9]+))?\\s+@@(.*)$");

    public List<DiffLine> parse(String patch) {
        List<DiffLine> lines = new ArrayList<>();
        if (patch == null || patch.isBlank()) {
            return lines;
        }

        int oldLine = 0;
        int newLine = 0;

        for (String raw : patch.split("\\R")) {
            if (raw.startsWith("@@")) {
                Matcher m = HUNK.matcher(raw);
                if (m.find()) {
                    oldLine = Integer.parseInt(m.group(1));
                    newLine = Integer.parseInt(m.group(3));
                }
                lines.add(DiffLine.builder()
                        .type("HUNK")
                        .content(raw)
                        .oldLineNumber(oldLine)
                        .newLineNumber(newLine)
                        .build());
                continue;
            }

            if (raw.isEmpty()) {
                continue;
            }

            char marker = raw.charAt(0);
            String content = raw.length() > 1 ? raw.substring(1) : "";

            switch (marker) {
                case '-' -> {
                    if (raw.startsWith("---")) {
                        continue;
                    }
                    lines.add(DiffLine.builder()
                            .type("REMOVED")
                            .content(content)
                            .oldLineNumber(oldLine)
                            .build());
                    oldLine++;
                }
                case '+' -> {
                    if (raw.startsWith("+++")) {
                        continue;
                    }
                    lines.add(DiffLine.builder()
                            .type("ADDED")
                            .content(content)
                            .newLineNumber(newLine)
                            .build());
                    newLine++;
                }
                case ' ' -> {
                    lines.add(DiffLine.builder()
                            .type("CONTEXT")
                            .content(content)
                            .oldLineNumber(oldLine)
                            .newLineNumber(newLine)
                            .build());
                    oldLine++;
                    newLine++;
                }
                default -> { /* skip */ }
            }
        }
        return lines;
    }
}
