package com.mergewise.context;
import com.mergewise.dto.PRFileChange;
import lombok.Data;
import java.util.*;

@Data
public class AgentContext {
 private String repo;
 private Integer prNumber;
 private List<String> files = new ArrayList<>();
 private List<PRFileChange> fileChanges = new ArrayList<>();
 private List<String> issues = new ArrayList<>();
 private List<String> suggestions = new ArrayList<>();
 private Map<String,Object> metadata = new HashMap<>();
 private boolean complete=false;
}