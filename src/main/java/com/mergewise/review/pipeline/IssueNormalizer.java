package com.mergewise.review.pipeline;

import com.mergewise.dto.ReviewIssue;
import com.mergewise.dto.review.ReviewIssueModel;
import com.mergewise.review.normalize.FindingNormalizer;
import com.mergewise.service.AiReviewSupport;
import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class IssueNormalizer {

    private final FindingNormalizer findingNormalizer;

    public IssueNormalizer(FindingNormalizer findingNormalizer) {
        this.findingNormalizer = findingNormalizer;
    }

    public NormalizedIssues normalize(List<ReviewIssue> rawIssues) {
        List<ReviewIssue> filtered = new ArrayList<>();
        int infrastructureSkipped = 0;

        if (rawIssues != null) {
            for (ReviewIssue issue : rawIssues) {
                if (AiReviewSupport.isInfrastructureIssue(
                        issue.getCategory(), issue.getFile(), issue.getTitle(), issue.getDescription())) {
                    infrastructureSkipped++;
                    continue;
                }
                filtered.add(issue);
            }
        }

        FindingNormalizer.NormalizationResult deduped = findingNormalizer.normalize(filtered);
        List<ReviewIssueModel> issues = deduped.getFindings().stream()
                .map(this::toModel)
                .toList();

        return NormalizedIssues.builder()
                .issues(issues)
                .rawCount(rawIssues != null ? rawIssues.size() : 0)
                .infrastructureSkipped(infrastructureSkipped)
                .deduplicatedFrom(deduped.getRawCount())
                .build();
    }

    private ReviewIssueModel toModel(com.mergewise.dto.review.CanonicalFinding f) {
        return ReviewIssueModel.builder()
                .id(f.getId())
                .fingerprint(f.getFingerprint())
                .severity(f.getSeverity())
                .severityColor(f.getSeverityColor())
                .category(f.getCategory())
                .explorerCategory(f.getTab())
                .file(f.getFile())
                .line(f.getLine())
                .title(f.getTitle())
                .description(f.getDescription())
                .rootCause(f.getRootCause())
                .productionImpact(f.getProductionImpact())
                .recommendation(f.getRecommendation())
                .fixedExample(f.getFixedExample())
                .confidence(f.getConfidence())
                .estimatedFixTime(f.getEstimatedFixTime())
                .autoFix(f.getAutoFixAvailable())
                .blocking(f.getBlocking())
                .references(f.getMergedFrom() != null ? new ArrayList<>(f.getMergedFrom()) : new ArrayList<>())
                .build();
    }

    @Data
    @Builder
    public static class NormalizedIssues {
        private List<ReviewIssueModel> issues;
        private int rawCount;
        private int infrastructureSkipped;
        private int deduplicatedFrom;
    }
}
