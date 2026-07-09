package com.mergewise.service;

import com.mergewise.context.AgentContext;
import com.mergewise.dto.PRAnalysisResponse;
import com.mergewise.review.pipeline.ReviewEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PRAnalysisResponseMapper {

    private final ReviewEngine reviewEngine;

    public PRAnalysisResponse fromContext(AgentContext context) {
        return reviewEngine.buildResponse(context);
    }
}
