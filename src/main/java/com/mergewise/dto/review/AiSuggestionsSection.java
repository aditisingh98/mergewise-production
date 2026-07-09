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
public class AiSuggestionsSection {

    @Builder.Default
    private List<SuggestionItem> quickWins = new ArrayList<>();

    @Builder.Default
    private List<SuggestionItem> codeQuality = new ArrayList<>();

    @Builder.Default
    private List<SuggestionItem> refactoring = new ArrayList<>();

    @Builder.Default
    private List<SuggestionItem> bestPractices = new ArrayList<>();

    @Builder.Default
    private List<SuggestionItem> readability = new ArrayList<>();

    @Builder.Default
    private List<SuggestionItem> logging = new ArrayList<>();

    @Builder.Default
    private List<SuggestionItem> testing = new ArrayList<>();

    @Builder.Default
    private List<SuggestionItem> architecture = new ArrayList<>();

    @Builder.Default
    private List<SuggestionItem> performance = new ArrayList<>();

    @Builder.Default
    private List<SuggestionItem> security = new ArrayList<>();
}
