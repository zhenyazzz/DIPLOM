package com.portfolio.identity.identity;

import com.portfolio.identity.api.identity.AdminUsersApi;
import com.portfolio.identity.api.identity.ChangeRoleRequest;
import com.portfolio.identity.api.identity.SetBlockedRequest;
import com.portfolio.identity.api.identity.UserResponse;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUsersController implements AdminUsersApi {

    private final AdminUserService adminUserService;

    @Override
    public ResponseEntity<List<UserResponse>> listUsers() {
        return ResponseEntity.ok(adminUserService.listUsers());
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
