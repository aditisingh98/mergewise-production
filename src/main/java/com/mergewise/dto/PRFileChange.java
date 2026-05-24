package com.mergewise.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PRFileChange {

 private String filename;

 private String status;

 private Integer additions;

 private Integer deletions;

 private Integer changes;

 private String previousFilename;

 private String patch;

 private List<String> addedLines;

 private List<String> removedLines;
 private String language;

 private String module;

 private String microservice;

 private Boolean backendCritical;

 private Integer riskScore;
}
