package com.portfolio.common.security.context;

import com.portfolio.common.security.jwt.JwtRoleClaims;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@RequiredArgsConstructor
public class SecuritySupport {

    private final String roleClaim;

    public UUID requireUserId() {
        Jwt jwt = requireJwt();
        String subject = jwt.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new UnauthenticatedException("JWT subject (sub) is missing");
        }
        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException ex) {
            throw new UnauthenticatedException("JWT subject (sub) is not a valid UUID: " + subject);
        }
    }

    /**
     * Primary role for callers that expect a single code (ADMIN preferred if present).
     */
    public String requireRole() {
        List<String> roles = requireRoles();
        String primary = JwtRoleClaims.primary(roles);
        if (primary == null) {
            throw new UnauthenticatedException("JWT role claim '" + roleClaim + "' is missing");
        }
        return primary;
    }

    public List<String> requireRoles() {
        Jwt jwt = requireJwt();
        Set<String> codes = new LinkedHashSet<>(JwtRoleClaims.extract(jwt, roleClaim));
        if (codes.isEmpty()) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null) {
                for (GrantedAuthority authority : authentication.getAuthorities()) {
                    String name = authority.getAuthority();
                    if (name != null && name.startsWith("ROLE_") && name.length() > 5) {
                        codes.add(name.substring(5));
                    }
                }
            }
        }
        if (codes.isEmpty()) {
            throw new UnauthenticatedException("JWT role claim '" + roleClaim + "' is missing");
        }
        return List.copyOf(codes);
    }

    public boolean hasRole(String role) {
        if (role == null || role.isBlank()) {
            return false;
        }
        return requireRoles().contains(role);
    }

    private Jwt requireJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthenticatedException("No authenticated principal");
        }
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getToken();
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            return jwt;
        }
        throw new UnauthenticatedException("Authenticated principal is not a JWT");
    }
}
