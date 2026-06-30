package com.mergewise.dto;

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
public class ScoreBreakdown {

    private int score;

    private String grade;

    private String summary;

    @Builder.Default
    private List<String> highlights = new ArrayList<>();
}
