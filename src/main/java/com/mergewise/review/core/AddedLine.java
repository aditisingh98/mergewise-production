package com.mergewise.review.core;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AddedLine {

    private final String filename;

    private final int patchLineNumber;

    private final String code;
}
