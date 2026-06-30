package com.mergewise.service;

import com.mergewise.context.AgentContext;
import com.mergewise.dto.PRAnalysisResponse;
import com.mergewise.dto.PRFileChange;
import com.mergewise.dto.PRRequest;
import com.mergewise.orchestrator.AgentOrchestrator;
import com.mergewise.service.GitHubPRParser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PRAnalysisService {

    private final GitHubService gitHubService;
    private final AgentOrchestrator orchestrator;
    private final AISummaryService aiSummaryService;
    private final PRAnalysisResponseMapper responseMapper;

    public PRAnalysisResponse analyze(@Valid PRRequest request) {
        String repo = GitHubPRParser.extractRepo(request.getPrUrl());
        Integer prNumber = GitHubPRParser.extractPRNumber(request.getPrUrl());

        AgentContext context = new AgentContext();
        context.setRepo(repo);
        context.setPrNumber(prNumber);

        List<PRFileChange> fileChanges = gitHubService.fetchFileChanges(repo, prNumber);
        context.setFileChanges(fileChanges);

        context = orchestrator.run(context);

        var summary = aiSummaryService.buildSummary(
                context,
                context.getFinalDecision(),
                context.getDecisionReasoning());

        return responseMapper.fromContext(context, summary);
    }
}
