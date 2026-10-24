package com.portfolio.core.api.identity;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Current-user profile contract ({@code GET /api/v1/users/me}).
 */
@RequestMapping("/api/v1/users")
public interface UsersApi {

    @GetMapping("/me")
    ResponseEntity<UserResponse> me();
}
