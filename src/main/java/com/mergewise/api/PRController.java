package com.mergewise.api;
import com.mergewise.context.AgentContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.mergewise.dto.PRRequest;
import com.mergewise.orchestrator.AgentOrchestrator;
import com.mergewise.service.GitHubPRParser;
import com.mergewise.service.GitHubService;

@RestController
@RequestMapping("/api/pr")
@RequiredArgsConstructor
public class PRController {
 private final GitHubService gitHubService;
 private final AgentOrchestrator orchestrator;

 @PostMapping("/analyze")
 public AgentContext analyze(@RequestBody PRRequest req){
  String repo = GitHubPRParser.extractRepo(req.getPrUrl());
  Integer prNumber = GitHubPRParser.extractPRNumber(req.getPrUrl());

  AgentContext context = new AgentContext();
  context.setRepo(repo);
  context.setPrNumber(prNumber);
  context.setFiles(gitHubService.fetchFiles(repo, prNumber));

  return orchestrator.run(context);
 }
}