package com.portfolio.identity.api.identity;

import java.util.UUID;

/**
 * Public user profile representation.
 */
public record UserResponse(
        UUID id,
        String email,
        String displayName,
        UserRole role,
        boolean blocked
) {
}
