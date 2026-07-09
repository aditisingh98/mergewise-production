package com.mergewise.dto.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewSummaries {

    private String executive;

    private String developer;

    private String reviewer;

    private String releaseManager;
}
