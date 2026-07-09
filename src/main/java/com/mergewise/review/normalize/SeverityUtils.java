package com.mergewise.review.normalize;

public final class SeverityUtils {

    private SeverityUtils() {
    }

    public static String colorFor(String severity) {
        if (severity == null) {
            return "gray";
        }
        return switch (severity.toUpperCase()) {
            case "CRITICAL" -> "red";
            case "HIGH" -> "orange";
            case "MEDIUM" -> "yellow";
            case "LOW" -> "blue";
            default -> "gray";
        };
    }

    public static int rank(String severity) {
        if (severity == null) {
            return 0;
        }
        return switch (severity.toUpperCase()) {
            case "CRITICAL" -> 5;
            case "HIGH" -> 4;
            case "MEDIUM" -> 3;
            case "LOW" -> 2;
            case "INFO" -> 1;
            default -> 0;
        };
    }

    public static String maxSeverity(String a, String b) {
        return rank(a) >= rank(b) ? normalize(a) : normalize(b);
    }

    public static String normalize(String severity) {
        if (severity == null || severity.isBlank()) {
            return "INFO";
        }
        return severity.toUpperCase();
    }

    public static boolean isBlocking(String severity) {
        String s = normalize(severity);
        return "CRITICAL".equals(s) || "HIGH".equals(s);
    }

    public static int penalty(String severity) {
        return switch (normalize(severity)) {
            case "CRITICAL" -> 25;
            case "HIGH" -> 15;
            case "MEDIUM" -> 8;
            case "LOW" -> 3;
            default -> 1;
        };
    }

    public static String grade(int score) {
        if (score >= 90) {
            return "A";
        }
        if (score >= 80) {
            return "B";
        }
        if (score >= 70) {
            return "C";
        }
        if (score >= 60) {
            return "D";
        }
        return "F";
    }

    public static String meaningForScore(int score) {
        if (score <= 20) {
            return "Very dangerous. Do not merge.";
        }
        if (score <= 40) {
            return "High risk. Major fixes required.";
        }
        if (score <= 60) {
            return "Needs work. Merge only after review.";
        }
        if (score <= 80) {
            return "Good. Minor issues remain.";
        }
        if (score <= 90) {
            return "Very good. Production ready with small improvements.";
        }
        return "Excellent. Safe to merge.";
    }

    public static String estimatedFixTime(String severity) {
        return switch (normalize(severity)) {
            case "CRITICAL" -> "2-8 hours";
            case "HIGH" -> "1-4 hours";
            case "MEDIUM" -> "30-90 minutes";
            case "LOW" -> "15-30 minutes";
            default -> "5-15 minutes";
        };
    }
}
