package com.mergewise.service;

public class AiProviderException extends RuntimeException {

    private final String status;
    private final Integer httpStatus;

    public AiProviderException(String status, String message) {
        this(status, null, message);
    }

    public AiProviderException(String status, Integer httpStatus, String message) {
        super(message);
        this.status = status;
        this.httpStatus = httpStatus;
    }

    public String getStatus() {
        return status;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }
}
