package com.mergewise.review.pipeline;

import com.mergewise.context.AgentContext;
import com.mergewise.dto.review.AiReviewStatus;
import com.mergewise.dto.review.SystemStatus;
import com.mergewise.dto.review.VcsStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SystemStatusBuilder {

    public SystemStatus build(AgentContext context) {
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        AiReviewStatus aiReview = buildAiStatus(context, warnings);
        VcsStatus vcs = buildVcsStatus(context);

        Object duration = context.getMetadata().get("analysisDurationMs");
        if (context.getMetadata().get("aiReviewMessage") != null
                && !"COMPLETED".equals(context.getMetadata().get("aiReviewStatus"))) {
            warnings.add(String.valueOf(context.getMetadata().get("aiReviewMessage")));
        }

        return SystemStatus.builder()
                .aiReview(aiReview)
                .vcs(vcs)
                .analysisDurationMs(duration instanceof Number n ? n.longValue() : null)
                .warnings(warnings)
                .errors(errors)
                .build();
    }

    @SuppressWarnings("unchecked")
    private AiReviewStatus buildAiStatus(AgentContext context, List<String> warnings) {
        Object stored = context.getMetadata().get("aiReviewStatusObject");
        if (stored instanceof AiReviewStatus status) {
            if ("FAILED".equals(status.getStatus())) {
                warnings.add(status.getImpact());
            }
            return status;
        }

        String status = stringMeta(context, "aiReviewStatus", "UNKNOWN");
        return AiReviewStatus.builder()
                .status(status)
                .provider(stringMeta(context, "aiReviewProvider", null))
                .httpStatus(intMeta(context, "aiReviewHttpStatus"))
                .errorCode(status)
                .retryable("RATE_LIMITED".equals(status))
                .fallbackUsed(Boolean.TRUE.equals(context.getMetadata().get("aiReviewFallbackUsed")))
                .impact("FAILED".equals(status)
                        ? "Static analysis completed successfully but AI insights are unavailable."
                        : "AI review status: " + status)
                .message(stringMeta(context, "aiReviewMessage", null))
                .build();
    }

    private VcsStatus buildVcsStatus(AgentContext context) {
        return VcsStatus.builder()
                .provider(stringMeta(context, "vcsProvider", "UNKNOWN"))
                .status("OK")
                .authMode(stringMeta(context, "authMode", "PUBLIC"))
                .message("Source control fetch completed.")
                .build();
    }

    private String stringMeta(AgentContext context, String key, String defaultValue) {
        Object value = context.getMetadata().get(key);
        return value != null ? value.toString() : defaultValue;
    }

    private Integer intMeta(AgentContext context, String key) {
        Object value = context.getMetadata().get(key);
        if (value instanceof Number n) {
            return n.intValue();
        }
        return null;
    }
}
