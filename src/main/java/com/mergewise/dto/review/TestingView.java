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
public class TestingView {

    private String summary;

    private String coverageEstimate;

    @Builder.Default
    private List<String> issueIds = new ArrayList<>();

    @Builder.Default
    private List<String> recommendationIds = new ArrayList<>();
}
