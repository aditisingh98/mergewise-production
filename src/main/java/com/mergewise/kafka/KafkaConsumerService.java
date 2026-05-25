package com.mergewise.kafka;

import com.mergewise.context.AgentContext;
import com.mergewise.dto.PRFileChange;
import com.mergewise.dto.PRRequest;
import com.mergewise.orchestrator.AgentOrchestrator;
import com.mergewise.service.GitHubPRParser;
import com.mergewise.service.GitHubService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerService {

 private final AgentOrchestrator orchestrator;

 private final GitHubService gitHubService;

 @KafkaListener(
         topics = "pr-analysis",
         groupId = "mergewise-group"
 )
 public void consume(PRRequest req) {

  try {

   log.info("Received PR URL: {}", req.getPrUrl());

   String repo =
           GitHubPRParser.extractRepo(req.getPrUrl());

   Integer prNumber =
           GitHubPRParser.extractPRNumber(req.getPrUrl());

   log.info(
           "Repo = {}, PR Number = {}",
           repo,
           prNumber
   );

   AgentContext context = new AgentContext();

   context.setRepo(repo);

   context.setPrNumber(prNumber);

   List<PRFileChange> fileChanges =
           gitHubService.fetchFileChanges(repo, prNumber);

   context.setFileChanges(fileChanges);

   orchestrator.run(context);

   log.info("Final Analysis = {}", context);

  } catch (Exception ex) {

   log.error(
           "PR analysis failed: {}",
           ex.getMessage(),
           ex
   );
  }
 }
}