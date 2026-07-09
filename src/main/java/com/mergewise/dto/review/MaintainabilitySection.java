package com.mergewise.dto.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintainabilitySection {

    private String maintainabilitySummary;

    private int issueCount;

    @Builder.Default
    private List<CanonicalFinding> maintainabilityIssues = new ArrayList<>();
}
