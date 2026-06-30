package com.mergewise.review.testing;

import com.mergewise.context.AgentContext;
import com.mergewise.dto.ArchitectureRecommendation;
import com.mergewise.dto.PRFileChange;
import com.mergewise.dto.TestingRecommendation;
import com.mergewise.review.core.PatchLineScanner;
import com.mergewise.review.core.ReviewAnalyzer;
import com.mergewise.review.core.ReviewIssueFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class TestingReviewAnalyzer implements ReviewAnalyzer {

    @Override
    public String category() {
        return "TESTING";
    }

    @Override
    public void analyze(AgentContext context) {
        Set<String> productionFiles = new HashSet<>();
        Set<String> testFiles = new HashSet<>();

        for (PRFileChange file : context.getFileChanges()) {
            String name = file.getFilename() != null ? file.getFilename() : "";
            if (PatchLineScanner.isTestFile(file)) {
                testFiles.add(name);
                continue;
            }
            if (PatchLineScanner.isJavaFile(file) && isProductionCode(name)) {
                productionFiles.add(name);
            }
        }

        for (String prodFile : productionFiles) {
            String expectedTest = inferTestFileName(prodFile);
            if (!testFiles.contains(expectedTest) && !hasRelatedTest(prodFile, testFiles)) {
                context.getReviewIssues().add(ReviewIssueFactory.issue(
                        "TESTING", "MEDIUM", prodFile, 0,
                        "Missing unit test coverage",
                        "Production code changed without accompanying test file in PR",
                        "No test file detected for " + prodFile,
                        "Untested changes increase regression risk in production",
                        "Add unit tests covering happy path, edge cases, and negative scenarios",
                        "@Test void shouldHandleInvalidInput() { assertThrows(...); }",
                        82));

                TestingRecommendation rec = TestingRecommendation.builder()
                        .testType("UNIT")
                        .file(prodFile)
                        .scenario("Happy path and validation failures")
                        .recommendation("Add tests in " + expectedTest)
                        .priority("HIGH")
                        .build();
                storeTestingRecommendation(context, rec);
            }
        }

        if (!productionFiles.isEmpty() && testFiles.isEmpty()) {
            context.getReviewIssues().add(ReviewIssueFactory.issue(
                    "TESTING", "HIGH", productionFiles.iterator().next(), 0,
                    "No tests in pull request",
                    "PR modifies production code without any test changes",
                    "Zero test files in diff",
                    "High risk of undetected regressions",
                    "Add unit and integration tests before merge",
                    "@SpringBootTest class FeatureIntegrationTest { }",
                    88));

            storeTestingRecommendation(context, TestingRecommendation.builder()
                    .testType("INTEGRATION")
                    .file("—")
                    .scenario("End-to-end API flow for changed endpoints")
                    .recommendation("Add integration test covering changed API contracts")
                    .priority("HIGH")
                    .build());
        }

        for (PRFileChange file : context.getFileChanges()) {
            if (file.getFilename() != null && file.getFilename().contains("Controller")) {
                storeTestingRecommendation(context, TestingRecommendation.builder()
                        .testType("INTEGRATION")
                        .file(file.getFilename())
                        .scenario("Negative test cases (invalid input, auth failures)")
                        .recommendation("Add MockMvc tests for error responses and validation")
                        .priority("MEDIUM")
                        .build());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void storeTestingRecommendation(AgentContext context, TestingRecommendation rec) {
        List<TestingRecommendation> list = (List<TestingRecommendation>) context.getMetadata()
                .computeIfAbsent("testingRecommendations", k -> new ArrayList<TestingRecommendation>());
        list.add(rec);
    }

    private boolean isProductionCode(String name) {
        return !name.contains("dto") && !name.contains("config") && name.endsWith(".java");
    }

    private String inferTestFileName(String prodFile) {
        String base = prodFile.replace("src/main/java/", "src/test/java/");
        return base.replace(".java", "Test.java");
    }

    private boolean hasRelatedTest(String prodFile, Set<String> testFiles) {
        String simpleName = prodFile.substring(prodFile.lastIndexOf('/') + 1).replace(".java", "");
        for (String test : testFiles) {
            if (test.contains(simpleName)) {
                return true;
            }
        }
        return false;
    }
}
