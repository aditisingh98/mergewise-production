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
public class TestingSection {

    private String testingSummary;

    private String coverageEstimate;

    @Builder.Default
    private List<SuggestionItem> missingTests = new ArrayList<>();

    @Builder.Default
    private List<SuggestionItem> unitTests = new ArrayList<>();

    @Builder.Default
    private List<SuggestionItem> integrationTests = new ArrayList<>();

    @Builder.Default
    private List<SuggestionItem> mockTests = new ArrayList<>();

    @Builder.Default
    private List<SuggestionItem> edgeCases = new ArrayList<>();

    @Builder.Default
    private List<SuggestionItem> regressionTests = new ArrayList<>();
}
