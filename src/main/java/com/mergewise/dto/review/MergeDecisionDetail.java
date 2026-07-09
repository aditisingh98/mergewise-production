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
public class MergeDecisionDetail {

    private String decision;

    private String reason;

    private int confidence;

    private String estimatedTimeToMerge;

    @Builder.Default
    private List<String> blockingIssueIds = new ArrayList<>();

    @Builder.Default
    private List<String> nextSteps = new ArrayList<>();
}
