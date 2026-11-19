package com.tollfinder.exception;

import org.springframework.http.HttpStatus;

/** The routing or geocoding provider failed; carries the HTTP status we should answer with. */
public class ExternalServiceException extends RuntimeException {

    private final HttpStatus status;

    public ExternalServiceException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
