package com.mergewise.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArchitectureRecommendation {

    private String pattern;

    private String file;

    private String observation;

    private String recommendation;

    private String impact;
}
