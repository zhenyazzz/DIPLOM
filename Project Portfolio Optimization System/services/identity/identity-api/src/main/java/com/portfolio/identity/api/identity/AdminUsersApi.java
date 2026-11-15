package com.portfolio.identity.api.identity;

import com.portfolio.common.web.web.PageResponse;
import com.portfolio.identity.api.identity.dto.ChangeRoleRequest;
import com.portfolio.identity.api.identity.dto.SetBlockedRequest;
import com.portfolio.identity.api.identity.dto.UserResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Admin user-management contract ({@code /api/v1/admin/users/**}).
 */
@RequestMapping("/api/v1/admin/users")
public interface AdminUsersApi {

    @GetMapping
    ResponseEntity<PageResponse<UserResponse>> listUsers(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(name = "filter", required = false) String filter);

    @PutMapping("/{userId}/role")
    ResponseEntity<UserResponse> changeRole(
            @PathVariable UUID userId,
            @Valid @RequestBody ChangeRoleRequest request);

    @PutMapping("/{userId}/blocked")
    ResponseEntity<UserResponse> setBlocked(
            @PathVariable UUID userId,
            @Valid @RequestBody SetBlockedRequest request);
}
