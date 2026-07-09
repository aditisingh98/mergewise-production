package com.mergewise.agents.review;

import com.mergewise.agents.core.Agent;
import com.mergewise.context.AgentContext;
import com.mergewise.dto.review.AiReviewStatus;
import com.mergewise.review.pipeline.AIReviewService;
import com.mergewise.review.pipeline.StaticAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
@Slf4j
@RequiredArgsConstructor
public class CodeReviewAgent implements Agent {

    private final StaticAnalysisService staticAnalysisService;
    private final AIReviewService aiReviewService;

    @Override
    public String getName() {
        return "CODE_REVIEW";
    }

    @Override
    public void execute(AgentContext context) {
        if (context.getReviewIssues() == null) {
            context.setReviewIssues(new ArrayList<>());
        }

        context.getReviewIssues().addAll(staticAnalysisService.analyze(context));

        AIReviewService.AiReviewResult aiResult = aiReviewService.review(context);
        if (aiResult.getIssues() != null) {
            context.getReviewIssues().addAll(aiResult.getIssues());
        }

        AiReviewStatus status = aiResult.getStatus();
        if (status != null) {
            context.getMetadata().put("aiReviewStatusObject", status);
            context.getMetadata().put("aiReviewStatus", status.getStatus());
            context.getMetadata().put("aiReviewMessage", status.getMessage());
            context.getMetadata().put("aiReviewProvider", status.getProvider());
            context.getMetadata().put("aiReviewHttpStatus", status.getHttpStatus());
            context.getMetadata().put("aiReviewFallbackUsed", status.getFallbackUsed());
            context.getMetadata().put("aiReviewEnabled", !"DISABLED".equals(status.getStatus()));
        }
    }
}
