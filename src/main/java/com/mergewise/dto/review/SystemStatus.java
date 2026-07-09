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
public class SystemStatus {

    private AiReviewStatus aiReview;

    private VcsStatus vcs;

    private Long analysisDurationMs;

    @Builder.Default
    private List<String> warnings = new ArrayList<>();

    @Builder.Default
    private List<String> errors = new ArrayList<>();
}
