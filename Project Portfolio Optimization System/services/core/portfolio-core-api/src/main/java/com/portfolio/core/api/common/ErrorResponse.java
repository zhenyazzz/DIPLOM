package com.portfolio.core.api.common;

import java.util.List;

/**
 * Uniform API error payload.
 */
public record ErrorResponse(
        String code,
        String message,
        List<FieldError> details
) {

    public ErrorResponse(String code, String message) {
        this(code, message, List.of());
    }

    /**
     * Field-level validation detail.
     */
    public record FieldError(String field, String message) {
    }
}
