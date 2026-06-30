package com.mergewise.context;

import com.mergewise.dto.PRFileChange;
import com.mergewise.dto.ReviewIssue;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class AgentContext {

 private String repo;

 private Integer prNumber;

 // ALL PR FILE CHANGES

 private List<PRFileChange> fileChanges = new ArrayList<>();

 // STRUCTURED REVIEW ISSUES

 private List<ReviewIssue> reviewIssues = new ArrayList<>();

 // SHARED AGENT METADATA

 private Map<String, Object> metadata = new HashMap<>();

 // FLOW CONTROL

 private boolean complete = false;

 // REVIEW SUMMARY

 private Integer overallScore = 0;

 private String riskLevel;

 private String finalDecision;

 private String decisionReasoning;
}