package com.portfolio.identity.api.auth;

/**
 * Access-token response. Refresh token is delivered via HttpOnly cookie, not in this body.
 *
 * @param accessToken JWT access token
 * @param tokenType   typically {@code "Bearer"}
 * @param expiresIn   access TTL in seconds
 */
public record TokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {

    public TokenResponse(String accessToken, long expiresIn) {
        this(accessToken, "Bearer", expiresIn);
    }
}
