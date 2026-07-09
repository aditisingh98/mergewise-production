package com.mergewise.dto.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueExplorerSection {

    private IssueCategoryView security;

    private IssueCategoryView runtime;

    private IssueCategoryView performance;

    private IssueCategoryView maintainability;

    private IssueCategoryView testing;

    private IssueCategoryView architecture;
}
