package com.portfolio.identity.api.identity;

/**
 * Admin payload to block or unblock a user.
 */
public record SetBlockedRequest(
        boolean blocked
) {
}
