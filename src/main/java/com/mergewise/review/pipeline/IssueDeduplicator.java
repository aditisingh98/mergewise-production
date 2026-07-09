package com.mergewise.review.pipeline;

import com.mergewise.dto.ReviewIssue;
import org.springframework.stereotype.Service;

@Service
public class IssueDeduplicator {

    private final IssueNormalizer issueNormalizer;

    public IssueDeduplicator(IssueNormalizer issueNormalizer) {
        this.issueNormalizer = issueNormalizer;
    }

    public IssueNormalizer.NormalizedIssues deduplicate(List<ReviewIssue> rawIssues) {
        return issueNormalizer.normalize(rawIssues);
    }
}
