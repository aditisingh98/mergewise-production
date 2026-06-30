package com.mergewise.kafka;

import com.mergewise.dto.PRRequest;
import com.mergewise.service.PRAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "mergewise.kafka.enabled", havingValue = "true")
public class KafkaConsumerService {

    private final PRAnalysisService prAnalysisService;

    @KafkaListener(
            topics = "pr-analysis",
            groupId = "mergewise-group"
    )
    public void consume(PRRequest req) {
        try {
            log.info("Received PR URL: {}", req.getPrUrl());
            var response = prAnalysisService.analyze(req);
            log.info("PR analysis completed. Decision={}, Issues={}",
                    response.getFinalDecision(),
                    response.getReviewIssues() != null ? response.getReviewIssues().size() : 0);
        } catch (Exception ex) {
            log.error("PR analysis failed: {}", ex.getMessage(), ex);
        }
    }
}
