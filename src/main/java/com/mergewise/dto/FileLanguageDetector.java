package com.mergewise.dto;

public class FileLanguageDetector {

    public static String detect(String fileName) {

        if (fileName == null) {
            return "unknown";
        }

        if (fileName.endsWith(".java")) {
            return "java";
        }

        if (fileName.endsWith(".kt")) {
            return "kotlin";
        }

        if (fileName.endsWith(".sql")) {
            return "sql";
        }

        if (fileName.endsWith(".yml")
                || fileName.endsWith(".yaml")) {
            return "yaml";
        }

        if (fileName.endsWith(".js")) {
            return "javascript";
        }

        if (fileName.endsWith(".ts")) {
            return "typescript";
        }

        if (fileName.endsWith(".py")) {
            return "python";
        }

        if (fileName.endsWith(".sh")) {
            return "shell";
        }

        if (fileName.endsWith(".tf")) {
            return "terraform";
        }
        if (fileName.endsWith(".jsx")) {
            return "react";
        }

        if (fileName.endsWith(".tsx")) {
            return "react-typescript";
        }

        if (fileName.equalsIgnoreCase("package.json")) {
            return "node";
        }

        if (fileName.equalsIgnoreCase("next.config.js")) {
            return "nextjs";
        }

        if (fileName.contains("tailwind")) {
            return "tailwind";
        }

        return "unknown";
    }
}