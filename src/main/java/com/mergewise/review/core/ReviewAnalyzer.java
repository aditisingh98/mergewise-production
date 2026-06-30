package com.mergewise.review.core;

import com.mergewise.context.AgentContext;

public interface ReviewAnalyzer {

    String category();

    void analyze(AgentContext context);
}
