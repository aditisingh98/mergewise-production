package com.mergewise.service;

public class AiProviderException extends RuntimeException {

    private final String status;

    public AiProviderException(String status, String message) {
        super(message);
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
