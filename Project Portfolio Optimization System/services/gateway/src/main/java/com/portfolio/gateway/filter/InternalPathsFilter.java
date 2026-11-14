package com.portfolio.gateway.filter;

import com.portfolio.gateway.web.ProblemResponses;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Internal-only APIs are never exposed through the gateway: {@code /api/v1/files} and {@code /api/v1/files/**}
 * (FTS) → 404 Problem Details {@code NOT_FOUND}, regardless of the token. Runs before Spring Security.
 */
@Component
public class InternalPathsFilter extends OncePerRequestFilter implements Ordered {

    public static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 10;

    private static final String FILES_PATH = "/api/v1/files";

    private final ProblemResponses problems;

    public InternalPathsFilter(ProblemResponses problems) {
        this.problems = problems;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = requestPath(request);
        if (path.equals(FILES_PATH) || path.startsWith(FILES_PATH + "/")) {
            problems.write(response, HttpStatus.NOT_FOUND, "NOT_FOUND", "Not found");
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
}
