package com.portfolio.core.api.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Self-registration payload (creates an Initiator).
 */
public record RegisterRequest(
        @Email
        @NotBlank
        String email,

        @NotBlank
        @Size(min = 1, max = 120)
        String displayName,

        @NotBlank
        @Size(min = 8, max = 128)
        String password
) {
}
