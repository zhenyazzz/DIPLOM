package com.portfolio.identity.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class IdentityException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    private IdentityException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public static IdentityException unauthorized(String message) {
        return new IdentityException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", message);
    }

    public static IdentityException conflict(String message) {
        return new IdentityException(HttpStatus.CONFLICT, "CONFLICT", message);
    }

    public static IdentityException notFound(String message) {
        return new IdentityException(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }

    public static IdentityException badRequest(String message) {
        return new IdentityException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message);
    }
}
