package com.mergewise.service;

import com.mergewise.context.AgentContext;
import com.mergewise.dto.PRAnalysisResponse;
import com.mergewise.dto.PRFileChange;
import com.mergewise.dto.PRRequest;
import com.mergewise.orchestrator.AgentOrchestrator;
import com.mergewise.vcs.GitLabMRParser;
import com.mergewise.vcs.GitLabMergeRequestRef;
import com.mergewise.vcs.VcsProvider;
import com.mergewise.vcs.VcsProviderDetector;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PRAnalysisService {

    private final GitHubService gitHubService;
    private final GitLabService gitLabService;
    private final AgentOrchestrator orchestrator;
    private final AISummaryService aiSummaryService;
    private final PRAnalysisResponseMapper responseMapper;

    public PRAnalysisResponse analyze(PRRequest request) {
        return analyze(request, null);
    }

    public PRAnalysisResponse analyze(@Valid PRRequest request, String authorizationHeader) {
        VcsProvider provider = VcsProviderDetector.detect(request.getPrUrl());

        AgentContext context = new AgentContext();
        context.getMetadata().put("vcsProvider", provider.name());

        List<PRFileChange> fileChanges = switch (provider) {
            case GITHUB -> fetchGitHubChanges(request, authorizationHeader, context);
            case GITLAB -> fetchGitLabChanges(request, authorizationHeader, context);
        };

        context.setFileChanges(fileChanges);
        context = orchestrator.run(context);

        var summary = aiSummaryService.buildSummary(
                context,
                context.getFinalDecision(),
                context.getDecisionReasoning());

        return responseMapper.fromContext(context, summary);
    }

    private List<PRFileChange> fetchGitHubChanges(
            PRRequest request,
            String authorizationHeader,
            AgentContext context) {

        String repo = GitHubPRParser.extractRepo(request.getPrUrl());
        Integer prNumber = GitHubPRParser.extractPRNumber(request.getPrUrl());

        String userToken = GitHubTokenResolver.resolve(
                request.getGithubToken(),
                authorizationHeader,
                null);

        context.setRepo(repo);
        context.setPrNumber(prNumber);
        context.getMetadata().put("usedRequestVcsToken", userToken != null);

        return gitHubService.fetchFileChanges(
                repo, prNumber, request.getGithubToken(), authorizationHeader);
    }

    private List<PRFileChange> fetchGitLabChanges(
            PRRequest request,
            String authorizationHeader,
            AgentContext context) {

        GitLabMergeRequestRef ref = GitLabMRParser.parse(request.getPrUrl());

        String userToken = GitHubTokenResolver.resolve(
                request.getGitlabToken(),
                authorizationHeader,
                null);

        context.setRepo(ref.projectPath());
        context.setPrNumber(ref.mergeRequestIid());
        context.getMetadata().put("gitlabHost", ref.host());
        context.getMetadata().put("usedRequestVcsToken", userToken != null);

        return gitLabService.fetchFileChanges(ref, request.getGitlabToken(), authorizationHeader);
    }
}
