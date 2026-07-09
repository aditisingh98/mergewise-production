package com.mergewise.dto.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class CanonicalFinding {

    private String id;

    private String fingerprint;

    private String title;

    private String severity;

    private String severityColor;

    private String category;

    private String tab;

    private String file;

    private Integer line;

    private String description;

    private String rootCause;

    private String productionImpact;

    private String recommendation;

    private String fixedExample;

    private Integer confidence;

    private String estimatedFixTime;

    private Boolean autoFixAvailable;

    private Boolean blocking;

    @Builder.Default
    private List<String> references = new ArrayList<>();

    @Builder.Default
    private List<String> mergedFrom = new ArrayList<>();

    private String owaspCategory;

    private String cwe;

    private Double cvss;

    private String exploitability;

    private String fixPriority;

    private String runtimeType;
}
