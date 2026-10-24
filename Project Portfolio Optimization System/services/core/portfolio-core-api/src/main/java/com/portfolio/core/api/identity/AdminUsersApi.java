package com.portfolio.core.api.identity;

import com.portfolio.core.api.identity.UserResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Admin user-management contract ({@code /api/v1/admin/users/**}).
 */
@RequestMapping("/api/v1/admin/users")
public interface AdminUsersApi {

    @GetMapping
    ResponseEntity<List<UserResponse>> listUsers();

    @PutMapping("/{userId}/role")
    ResponseEntity<UserResponse> changeRole(
            @PathVariable UUID userId,
            @Valid @RequestBody ChangeRoleRequest request);

    @PutMapping("/{userId}/blocked")
    ResponseEntity<UserResponse> setBlocked(
            @PathVariable UUID userId,
            @Valid @RequestBody SetBlockedRequest request);
}
