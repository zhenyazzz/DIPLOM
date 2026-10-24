package com.portfolio.core.api.identity;

/**
 * Admin payload to block or unblock a user.
 */
public record SetBlockedRequest(
        boolean blocked
) {
}
