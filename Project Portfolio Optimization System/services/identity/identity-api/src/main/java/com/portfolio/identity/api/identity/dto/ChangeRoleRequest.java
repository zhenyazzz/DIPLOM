package com.portfolio.identity.api.identity.dto;

import com.portfolio.identity.api.identity.UserRole;
import jakarta.validation.constraints.NotNull;

/**
 * Admin payload to assign a single role (users.role_id → roles).
 */
public record ChangeRoleRequest(
        @NotNull
        UserRole role
) {
}
