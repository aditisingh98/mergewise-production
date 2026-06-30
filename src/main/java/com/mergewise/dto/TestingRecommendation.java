package com.mergewise.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestingRecommendation {

    private String testType;

    private String file;

    private String scenario;

    private String recommendation;

    private String priority;
}
