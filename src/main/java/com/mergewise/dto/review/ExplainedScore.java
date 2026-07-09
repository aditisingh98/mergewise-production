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
public class ExplainedScore {

    private int score;

    private String grade;

    private String meaning;

    private String howCalculated;

    @Builder.Default
    private List<ScoreDeduction> deductions = new ArrayList<>();

    @Builder.Default
    private List<String> howToImprove = new ArrayList<>();
}
