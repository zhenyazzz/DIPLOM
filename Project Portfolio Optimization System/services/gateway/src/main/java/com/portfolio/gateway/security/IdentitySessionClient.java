package com.portfolio.gateway.security;

/**
 * Asks Auth Service (identity) whether the access JWT still has an active Redis session.
 * Gateway never talks to Redis directly.
 */
public interface IdentitySessionClient {

    boolean isSessionActive(String rawAccessToken);
}
