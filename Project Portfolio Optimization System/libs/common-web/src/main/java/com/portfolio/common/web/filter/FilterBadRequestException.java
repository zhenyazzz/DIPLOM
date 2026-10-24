package com.portfolio.common.web.filter;

public class FilterBadRequestException extends RuntimeException {

    public FilterBadRequestException(String message) {
        super(message);
    }

    public FilterBadRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
