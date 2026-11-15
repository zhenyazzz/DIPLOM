package com.portfolio.common.security.jwt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Extracts role codes from JWT. Supports {@code roles} (array) and a single string claim
 * (default {@code role}) for backward compatibility.
 */
public final class JwtRoleClaims {

    private JwtRoleClaims() {}

    public static List<String> extract(Jwt jwt, String roleClaim) {
        Set<String> codes = new LinkedHashSet<>();
        addCollectionClaim(jwt, "roles", codes);
        if (roleClaim != null && !roleClaim.isBlank() && !"roles".equals(roleClaim)) {
            Object single = jwt.getClaim(roleClaim);
            if (single instanceof Collection<?> collection) {
                for (Object item : collection) {
                    addCode(codes, item);
                }
            } else if (single != null) {
                addCode(codes, single);
            }
        }
        if (codes.isEmpty()) {
            addCode(codes, jwt.getClaimAsString("role"));
        }
        return List.copyOf(codes);
    }

    private static void addCollectionClaim(Jwt jwt, String claim, Set<String> codes) {
        Object value = jwt.getClaim(claim);
        if (value instanceof Collection<?> collection) {
            for (Object item : collection) {
                addCode(codes, item);
            }
        }
    }

    private static void addCode(Set<String> codes, Object raw) {
        if (raw == null) {
            return;
        }
        String code = raw.toString().trim();
        if (!code.isEmpty()) {
            codes.add(code);
        }
    }

    public static String primary(List<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return null;
        }
        if (roleCodes.contains("ADMIN")) {
            return "ADMIN";
        }
        return new ArrayList<>(roleCodes).getFirst();
    }
}
