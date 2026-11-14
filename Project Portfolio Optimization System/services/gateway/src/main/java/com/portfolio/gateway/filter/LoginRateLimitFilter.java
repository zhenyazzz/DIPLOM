package com.portfolio.gateway.filter;

import com.portfolio.gateway.config.GatewayProperties;
import com.portfolio.gateway.web.ProblemResponses;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Fixed-window rate limit for {@code POST /api/v1/auth/login}, keyed by {@code request.getRemoteAddr()}
 * (real client IP thanks to {@code server.forward-headers-strategy: framework}). Over the limit →
 * 429 Problem Details {@code TOO_MANY_REQUESTS} with {@code Retry-After} (seconds until the window resets).
 * Other requests pass untouched. Runs before Spring Security.
 */
@Component
public class LoginRateLimitFilter extends OncePerRequestFilter implements Ordered {

    public static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 20;
    public static final String LOGIN_PATH = "/api/v1/auth/login";

    /** Above this many tracked clients, expired windows are purged before each insert. */
    private static final int MAX_TRACKED_CLIENTS = 10_000;

    private final GatewayProperties.LoginRateLimit limit;
    private final ProblemResponses problems;
    private final Clock clock;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public LoginRateLimitFilter(GatewayProperties properties, ProblemResponses problems, Clock clock) {
        this.limit = properties.loginRateLimit();
        this.problems = problems;
        this.clock = clock;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!"POST".equalsIgnoreCase(request.getMethod()) || !LOGIN_PATH.equals(requestPath(request))) {
            chain.doFilter(request, response);
            return;
        }

        long now = clock.millis();
        long windowMillis = limit.window().toMillis();

        if (windows.size() > MAX_TRACKED_CLIENTS) {
            windows.entrySet().removeIf(entry -> now - entry.getValue().startMillis() >= windowMillis);
        }

        Window window = windows.compute(request.getRemoteAddr(), (key, current) ->
                current == null || now - current.startMillis() >= windowMillis
                        ? new Window(now, 1)
                        : new Window(current.startMillis(), current.count() + 1));

        if (window.count() > limit.capacity()) {
            long remainingMillis = window.startMillis() + windowMillis - now;
            long retryAfterSeconds = Math.max(1L, (remainingMillis + 999L) / 1000L);
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            problems.write(response, HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_REQUESTS",
                    "Too many login attempts, try again later");
            return;
        }
        chain.doFilter(request, response);
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    /** Request URI without the context path prefix. */
    private static String requestPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
            return uri.substring(contextPath.length());
        }
        return uri;
    }

    /** Start of the current fixed window and number of attempts counted within it. */
    private record Window(long startMillis, int count) {}
}
