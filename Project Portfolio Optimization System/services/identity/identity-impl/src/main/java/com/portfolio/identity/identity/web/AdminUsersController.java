package com.portfolio.identity.identity.web;

import com.portfolio.common.web.web.PageResponse;
import com.portfolio.identity.api.identity.AdminUsersApi;
import com.portfolio.identity.api.identity.dto.ChangeRoleRequest;
import com.portfolio.identity.api.identity.dto.SetBlockedRequest;
import com.portfolio.identity.api.identity.dto.UserResponse;
import com.portfolio.identity.identity.service.AdminUserService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUsersController implements AdminUsersApi {

    private final AdminUserService adminUserService;

    @Override
    public ResponseEntity<PageResponse<UserResponse>> listUsers(Pageable pageable, String filter) {
        return ResponseEntity.ok(adminUserService.listUsers(pageable, filter));
    }

    @Override
    public ResponseEntity<UserResponse> changeRole(UUID userId, ChangeRoleRequest request) {
        return ResponseEntity.ok(adminUserService.changeRole(userId, request));
    }

    @Override
    public ResponseEntity<UserResponse> setBlocked(UUID userId, SetBlockedRequest request) {
        return ResponseEntity.ok(adminUserService.setBlocked(userId, request));
    }
}
