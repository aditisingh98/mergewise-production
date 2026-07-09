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
public class ArchitectureSection {

    private String architectureSummary;

    private int issueCount;

    @Builder.Default
    private List<CanonicalFinding> architectureIssues = new ArrayList<>();

    @Builder.Default
    private List<SuggestionItem> suggestions = new ArrayList<>();
}
