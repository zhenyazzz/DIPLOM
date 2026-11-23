package com.portfolio.identity.api.auth;

import java.util.UUID;

/**
 * Confirms an access JWT has an active Redis session.
 *
 * @param jti    JWT id stored as the Redis session key
 * @param userId subject from the access token
 * @param role   role code from the access token / session payload
 */
public record AccessSessionResponse(String jti, UUID userId, String role) {
}
