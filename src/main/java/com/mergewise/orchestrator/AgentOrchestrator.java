package com.mergewise.orchestrator;
import org.springframework.stereotype.Service;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.mergewise.agents.core.Agent;
import com.mergewise.context.AgentContext;

@Service
public class AgentOrchestrator {
 private final Map<String,Agent> agentMap;

 public AgentOrchestrator(List<Agent> agents) {
  this.agentMap = agents.stream()
          .collect(Collectors.toMap(Agent::getName, Function.identity()));
 }

 public AgentContext run(AgentContext c){
  Set<String> executedAgents = new HashSet<>();

  while(!c.isComplete()){
   getAgent("PLANNER").execute(c);

   String next=(String)c.getMetadata().get("next");
   if(next==null){c.setComplete(true);break;}

   if(!executedAgents.add(next)){
    c.getMetadata().remove("next");
    c.setComplete(true);
    break;
   }

   getAgent(next).execute(c);
   c.getMetadata().remove("next");
  }

  return c;
 }

 private Agent getAgent(String name) {
  Agent agent = agentMap.get(name);
  if(agent == null){
   throw new IllegalStateException("No agent registered with name: " + name);
  }
  return agent;
 }
}