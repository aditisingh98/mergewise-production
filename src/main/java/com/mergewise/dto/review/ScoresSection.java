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
public class ScoresSection {

    private ExplainedScore overall;

    private ExplainedScore quality;

    private ExplainedScore security;

    private ExplainedScore performance;

    private ExplainedScore maintainability;

    private ExplainedScore complexity;

    private ExplainedScore mergeConfidence;
}
