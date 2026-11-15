package com.portfolio.identity.auth.service;

import com.portfolio.identity.config.AuthProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefreshCookieService {

    public static final String COOKIE_NAME = "refresh_token";
    public static final String COOKIE_PATH = "/api/v1/auth";

    private final AuthProperties authProperties;

    public void write(HttpServletResponse response, String rawRefreshToken) {
        Duration maxAge = authProperties.getJwt().getRefreshTokenTtl();
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(rawRefreshToken, maxAge).toString());
    }

    public void clear(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie("", Duration.ZERO).toString());
    }

    public Optional<String> read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(c -> COOKIE_NAME.equals(c.getName()))
                .map(Cookie::getValue)
                .filter(v -> v != null && !v.isBlank())
                .findFirst();
    }

    private ResponseCookie buildCookie(String value, Duration maxAge) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(authProperties.getCookie().isSecure())
                .path(COOKIE_PATH)
                .maxAge(maxAge)
                .sameSite("Lax")
                .build();
    }
}
