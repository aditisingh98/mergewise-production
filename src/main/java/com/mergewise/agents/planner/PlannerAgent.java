package com.mergewise.agents.planner;
import org.springframework.stereotype.Component;
import com.mergewise.agents.core.Agent;
import com.mergewise.context.AgentContext;

@Component
public class PlannerAgent implements Agent{
 public String getName(){return "PLANNER";}
 public void execute(AgentContext c){
  if(c.getIssues().isEmpty()){c.getMetadata().put("next","NPE");return;}
  if(c.getIssues().size()<3){c.getMetadata().put("next","QUALITY");return;}
  c.setComplete(true);
 }
}