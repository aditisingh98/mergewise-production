package com.mergewise.api;

import com.mergewise.context.AgentContext;
import com.mergewise.dto.PRFileChange;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.mergewise.dto.PRRequest;
import com.mergewise.orchestrator.AgentOrchestrator;
import com.mergewise.service.GitHubPRParser;
import com.mergewise.service.GitHubService;

import java.util.List;

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

  List<PRFileChange> fileChanges =
          gitHubService.fetchFileChanges(repo, prNumber);

  context.setFileChanges(fileChanges);

  return orchestrator.run(context);
 }
}