package com.mergewise.agents.npe;
import org.springframework.stereotype.Component;
import com.mergewise.agents.core.Agent;
import com.mergewise.context.AgentContext;

@Component
public class NpeAgent implements Agent{
 public String getName(){return "NPE";}
 public void execute(AgentContext c){
  for(String f:c.getFiles()){
   if(f.contains("get(")){c.getIssues().add("Possible NPE: "+f);}
  }
 }
}