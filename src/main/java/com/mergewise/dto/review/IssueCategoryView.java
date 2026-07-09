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
public class IssueCategoryView {

    private String summary;

    private int count;

    @Builder.Default
    private List<String> issueIds = new ArrayList<>();
}
