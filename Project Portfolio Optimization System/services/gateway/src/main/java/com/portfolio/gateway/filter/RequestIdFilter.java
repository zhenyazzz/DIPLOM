package com.portfolio.gateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Ensures every request has an {@code X-Request-Id}: reuses a valid incoming value or generates a UUID.
 * The id is put into MDC ({@link #MDC_KEY}), forwarded to core as a request header and echoed in the response.
 * Runs first, before Spring Security (order -100), so even 401/404/429 responses carry the id.
 */
@Component
public class RequestIdFilter extends OncePerRequestFilter implements Ordered {

    public static final String HEADER = "X-Request-Id";
    public static final String MDC_KEY = "requestId";
    public static final int ORDER = Ordered.HIGHEST_PRECEDENCE;

    private static final Pattern VALID_ID = Pattern.compile("[A-Za-z0-9._:-]{1,128}");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestId = resolveRequestId(request.getHeader(HEADER));
        response.setHeader(HEADER, requestId);
        MDC.put(MDC_KEY, requestId);
        try {
            chain.doFilter(new RequestIdRequestWrapper(request, requestId), response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    static String resolveRequestId(String incoming) {
        if (incoming != null && VALID_ID.matcher(incoming).matches()) {
            return incoming;
        }
        return UUID.randomUUID().toString();
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    /** Exposes the chosen request id as the {@link #HEADER} header, replacing any incoming value. */
    private static final class RequestIdRequestWrapper extends HttpServletRequestWrapper {

        private final String requestId;

        RequestIdRequestWrapper(HttpServletRequest request, String requestId) {
            super(request);
            this.requestId = requestId;
        }

        @Override
        public String getHeader(String name) {
            return isRequestIdHeader(name) ? requestId : super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            return isRequestIdHeader(name) ? Collections.enumeration(List.of(requestId)) : super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            Set<String> names = new LinkedHashSet<>();
            Enumeration<String> original = super.getHeaderNames();
            if (original != null) {
                while (original.hasMoreElements()) {
                    String name = original.nextElement();
                    if (!isRequestIdHeader(name)) {
                        names.add(name);
                    }
                }
            }
            names.add(HEADER);
            return Collections.enumeration(names);
        }

        private static boolean isRequestIdHeader(String name) {
            return HEADER.equalsIgnoreCase(name);
        }
    }
}
