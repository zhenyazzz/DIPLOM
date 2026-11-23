package com.portfolio.gateway.catalog.exception;

public class InvalidProjectIdException extends RuntimeException {

    public InvalidProjectIdException() {
        super("Invalid project id");
    }
}
