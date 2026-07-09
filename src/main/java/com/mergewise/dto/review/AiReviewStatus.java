package com.mergewise.dto.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiReviewStatus {

    private String status;

    private String provider;

    private Integer httpStatus;

    private String errorCode;

    private Boolean retryable;

    private Boolean fallbackUsed;

    private String impact;

    private String message;
}
