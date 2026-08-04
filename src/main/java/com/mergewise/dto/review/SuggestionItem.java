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
public class SuggestionItem {

    private String id;

    private String title;

    private String description;

    private String why;

    private String expectedBenefit;

    private String estimatedEffort;

    private String priority;

    private String file;

    private Integer line;

    private String affectedCode;

    private String oldCode;

    private String newCode;

    private String suggestedCode;
}
