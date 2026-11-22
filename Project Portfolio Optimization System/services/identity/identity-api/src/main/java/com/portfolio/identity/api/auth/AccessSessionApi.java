package com.portfolio.identity.api.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Active access-session checks for gateways (JWT alone is not enough – Redis must still hold the jti).
 */
@RequestMapping("/api/v1/auth/sessions")
public interface AccessSessionApi {

    @GetMapping("/active")
    ResponseEntity<AccessSessionResponse> active();
}
