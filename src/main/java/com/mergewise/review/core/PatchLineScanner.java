package com.mergewise.review.core;

import com.mergewise.dto.PRFileChange;

import java.util.ArrayList;
import java.util.List;

public final class PatchLineScanner {

    private PatchLineScanner() {
    }

    public static List<AddedLine> scanAddedLines(PRFileChange change) {
        List<AddedLine> lines = new ArrayList<>();
        if (change == null || change.getPatch() == null || change.getPatch().isBlank()) {
            return lines;
        }

        String filename = change.getFilename() != null ? change.getFilename() : "unknown";
        String[] patchLines = change.getPatch().split("\\R");

        for (int i = 0; i < patchLines.length; i++) {
            String line = patchLines[i];
            if (!line.startsWith("+") || line.startsWith("+++")) {
                continue;
            }
            String code = line.substring(1).trim();
            if (code.isBlank() || code.startsWith("//") || code.startsWith("*")) {
                continue;
            }
            lines.add(new AddedLine(filename, i + 1, code));
        }
        return lines;
    }

    public static boolean isJavaFile(PRFileChange change) {
        return change.getFilename() != null
                && change.getFilename().toLowerCase().endsWith(".java");
    }

    public static boolean isSqlFile(PRFileChange change) {
        String name = change.getFilename() != null ? change.getFilename().toLowerCase() : "";
        return name.endsWith(".sql") || name.contains("migration");
    }

    public static boolean isTestFile(PRFileChange change) {
        String name = change.getFilename() != null ? change.getFilename().toLowerCase() : "";
        return name.contains("test") || name.contains("spec");
    }
}
