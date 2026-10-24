package com.portfolio.core.api.identity;

import com.portfolio.core.api.identity.UserRole;
import jakarta.validation.constraints.NotNull;

/**
 * Admin payload to assign a user role.
 */
public record ChangeRoleRequest(
        @NotNull
        UserRole role
) {
}
