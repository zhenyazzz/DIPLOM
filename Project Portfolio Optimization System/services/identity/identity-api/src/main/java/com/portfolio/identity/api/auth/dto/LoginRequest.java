package com.portfolio.identity.api.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Credentials for sign-in.
 */
public record LoginRequest(
        @Email
        @NotBlank
        String email,

        @NotBlank
        String password
) {
}
