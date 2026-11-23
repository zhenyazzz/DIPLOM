package com.portfolio.identity.auth;

import com.portfolio.identity.api.auth.AccessSessionApi;
import com.portfolio.identity.api.auth.AccessSessionResponse;
import com.portfolio.identity.shared.ApiException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AccessSessionController implements AccessSessionApi {

    private final AccessSessionService accessSessionService;

    @Override
    public ResponseEntity<AccessSessionResponse> active() {
        Jwt jwt = currentJwt();
        String jti = jwt.getId();
        AccessSessionService.SessionPayload session = accessSessionService.requireActive(jti);
        if (session == null) {
            throw ApiException.unauthorized("Access session is not active");
        }
        UUID subject = UUID.fromString(jwt.getSubject());
        if (!subject.equals(session.userId())) {
            throw ApiException.unauthorized("Access session does not match token subject");
        }
        return ResponseEntity.ok(new AccessSessionResponse(jti, session.userId(), session.role()));
    }

    private static Jwt currentJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw ApiException.unauthorized("Authentication required");
        }
        if (jwt.getId() == null || jwt.getId().isBlank()) {
            throw ApiException.unauthorized("Access token has no jti");
        }
        return jwt;
    }
}
