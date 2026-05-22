package com.mergewise.dto;
import lombok.Data;

@Data
public class PRRequest {
 private String repo;
 private Integer prNumber;
 private String prUrl;

}