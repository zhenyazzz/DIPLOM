package com.portfolio.identity.api.identity.dto;

/**
 * Admin payload to block or unblock a user.
 */
public record SetBlockedRequest(
        boolean blocked
) {
}
