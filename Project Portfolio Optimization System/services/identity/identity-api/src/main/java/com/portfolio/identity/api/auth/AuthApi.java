package com.portfolio.identity.api.auth;

import com.portfolio.identity.api.identity.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Auth contract matching OpenAPI Auth operations ({@code /api/v1/auth/**}).
 * <p>
 * Refresh-token cookie read/write uses {@link HttpServletRequest}/{@link HttpServletResponse}
 * on login, refresh, and logout. Register returns {@code 201} from the implementing controller.
 */
@RequestMapping("/api/v1/auth")
public interface AuthApi {

    @PostMapping("/register")
    ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request);

    @PostMapping("/login")
    ResponseEntity<TokenResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response);

    @PostMapping("/refresh")
    ResponseEntity<TokenResponse> refresh(
            HttpServletRequest request,
            HttpServletResponse response);

    @PostMapping("/logout")
    ResponseEntity<Void> logout(
            HttpServletRequest request,
            HttpServletResponse response);
}
