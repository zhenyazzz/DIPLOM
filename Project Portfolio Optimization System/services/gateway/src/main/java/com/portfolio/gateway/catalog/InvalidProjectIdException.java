package com.portfolio.gateway.catalog;

public class InvalidProjectIdException extends RuntimeException {

    public InvalidProjectIdException() {
        super("Invalid project id");
    }
}
