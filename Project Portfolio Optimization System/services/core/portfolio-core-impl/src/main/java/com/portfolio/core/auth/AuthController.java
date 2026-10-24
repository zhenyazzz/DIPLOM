package com.portfolio.core.auth;

import com.portfolio.core.api.auth.AuthApi;
import com.portfolio.core.api.auth.LoginRequest;
import com.portfolio.core.api.auth.RegisterRequest;
import com.portfolio.core.api.auth.TokenResponse;
import com.portfolio.core.api.identity.UserResponse;
import com.portfolio.core.auth.AuthService;
import com.portfolio.core.auth.RefreshCookieService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final AuthService authService;
    private final RefreshCookieService refreshCookieService;

    @Override
    public ResponseEntity<UserResponse> register(RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @Override
    public ResponseEntity<TokenResponse> login(LoginRequest request, HttpServletResponse response) {
        AuthService.AuthResult result = authService.login(request);
        refreshCookieService.write(response, result.rawRefreshToken());
        return ResponseEntity.ok(result.tokens());
    }

    @Override
    public ResponseEntity<TokenResponse> refresh(HttpServletRequest request, HttpServletResponse response) {
        String raw = refreshCookieService.read(request).orElse(null);
        AuthService.AuthResult result = authService.refresh(raw);
        refreshCookieService.write(response, result.rawRefreshToken());
        return ResponseEntity.ok(result.tokens());
    }

    @Override
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        refreshCookieService.read(request).ifPresent(authService::logout);
        refreshCookieService.clear(response);
        return ResponseEntity.noContent().build();
    }
}
