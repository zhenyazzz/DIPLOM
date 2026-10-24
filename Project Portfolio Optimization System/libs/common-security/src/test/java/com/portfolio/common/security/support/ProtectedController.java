package com.portfolio.common.security.support;

import com.portfolio.common.security.context.SecuritySupport;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProtectedController {

    private final SecuritySupport securitySupport;

    public ProtectedController(SecuritySupport securitySupport) {
        this.securitySupport = securitySupport;
    }

    @GetMapping("/api/protected/me")
    public Map<String, String> me() {
        UUID userId = securitySupport.requireUserId();
        String role = securitySupport.requireRole();
        return Map.of("userId", userId.toString(), "role", role);
    }

    @GetMapping("/api/admin/secret")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, String> adminSecret() {
        return Map.of("scope", "admin");
    }
}
