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
public class VcsStatus {

    private String provider;

    private String status;

    private String authMode;

    private String message;
}
