package com.mergewise.service;

import com.mergewise.context.AgentContext;
import com.mergewise.dto.ExecutiveSummary;
import com.mergewise.dto.PRAnalysisResponse;
import com.mergewise.review.normalize.FindingNormalizer;
import com.mergewise.review.normalize.ReviewResponseAssembler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PRAnalysisResponseMapper {

    private final FindingNormalizer findingNormalizer;
    private final ReviewResponseAssembler reviewResponseAssembler;

    public PRAnalysisResponse fromContext(AgentContext context, ExecutiveSummary executiveSummary) {
        FindingNormalizer.NormalizationResult normalization = findingNormalizer.normalize(context.getReviewIssues());
        context.getMetadata().put("rawFindingCount", normalization.getRawCount());
        context.getMetadata().put("deduplicatedFindingCount", normalization.getDeduplicatedCount());
        return reviewResponseAssembler.assemble(context, normalization, executiveSummary);
    }
}
