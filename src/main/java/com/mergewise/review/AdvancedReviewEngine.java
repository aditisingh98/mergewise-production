package com.mergewise.review;

import com.mergewise.context.AgentContext;
import com.mergewise.review.core.ReviewAnalyzer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdvancedReviewEngine {

    private final List<ReviewAnalyzer> analyzers;

    public void run(AgentContext context) {
        if (context.getReviewIssues() == null) {
            context.setReviewIssues(new ArrayList<>());
        }

        log.info("Starting advanced PR review engine with {} analyzers", analyzers.size());

        for (ReviewAnalyzer analyzer : analyzers) {
            try {
                log.debug("Running analyzer: {}", analyzer.category());
                analyzer.analyze(context);
            } catch (Exception ex) {
                log.warn("Analyzer {} failed: {}", analyzer.category(), ex.getMessage());
                context.getMetadata().put("analyzerError_" + analyzer.category(), ex.getMessage());
            }
        }

        log.info("Advanced review engine completed. Total issues={}",
                context.getReviewIssues() != null ? context.getReviewIssues().size() : 0);
    }
}
