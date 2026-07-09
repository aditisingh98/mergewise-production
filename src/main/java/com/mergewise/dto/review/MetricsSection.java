package com.mergewise.dto.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricsSection {

    private int totalFindings;

    private int deduplicatedFrom;

    private int filesAnalyzed;

    private int linesAdded;

    private int linesRemoved;

    private int linesChanged;

    private int analyzersRun;

    private long analysisDurationMs;
}
