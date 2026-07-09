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
public class FileReviewRef {

    private String file;

    private int score;

    private String risk;

    private String summary;

    @Builder.Default
    private List<String> issueIds = new ArrayList<>();
}
