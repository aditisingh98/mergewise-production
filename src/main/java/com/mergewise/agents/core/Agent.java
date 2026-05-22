package com.mergewise.agents.core;
import com.mergewise.context.AgentContext;
public interface Agent {
 String getName();
 void execute(AgentContext context);
}