package com.portfolio.gateway.graphql;

import lombok.Getter;

@Getter
public class GraphqlSecurityException extends RuntimeException {

    private final String code;

    public GraphqlSecurityException(String code, String message) {
        super(message);
        this.code = code;
    }

    public static GraphqlSecurityException unauthorized(String message) {
        return new GraphqlSecurityException("UNAUTHORIZED", message);
    }

    public static GraphqlSecurityException forbidden(String message) {
        return new GraphqlSecurityException("FORBIDDEN", message);
    }
}
