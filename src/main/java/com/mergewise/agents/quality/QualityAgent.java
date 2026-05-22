package com.mergewise.agents.quality;
import org.springframework.stereotype.Component;
import com.mergewise.agents.core.Agent;
import com.mergewise.context.AgentContext;

@Component
public class QualityAgent implements Agent{
 public String getName(){return "QUALITY";}
 public void execute(AgentContext c){
  for(String f:c.getFiles()){
   if(f.length()>200){c.getSuggestions().add("Large method detected");}
  }
 }
}