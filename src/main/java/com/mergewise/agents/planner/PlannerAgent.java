package com.mergewise.agents.planner;
import org.springframework.stereotype.Component;
import com.mergewise.agents.core.Agent;
import com.mergewise.context.AgentContext;

@Component
public class PlannerAgent implements Agent{
 public String getName(){return "PLANNER";}
 public void execute(AgentContext c){
  if(!Boolean.TRUE.equals(c.getMetadata().get("codeReviewComplete"))){
   c.getMetadata().put("next","CODE_REVIEW");
   return;
  }
  c.getMetadata().remove("codeReviewComplete");
  c.setComplete(true);
 }
}