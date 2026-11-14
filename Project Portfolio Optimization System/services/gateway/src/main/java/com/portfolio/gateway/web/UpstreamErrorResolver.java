package com.portfolio.gateway.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

/**
 * Maps failures talking to core into Problem Details instead of a 500/Whitelabel page:
 * connection refused / unknown host → 502 {@code UPSTREAM_UNAVAILABLE}; read timeout → 504 {@code UPSTREAM_TIMEOUT}.
 * Any other exception → {@code null} (let default resolvers handle it).
 */
@Component
public class UpstreamErrorResolver implements HandlerExceptionResolver, Ordered {

    private static final int MAX_CAUSE_DEPTH = 20;

    private static final Upstream UNAVAILABLE =
            new Upstream(HttpStatus.BAD_GATEWAY, "UPSTREAM_UNAVAILABLE", "Upstream service is unavailable");
    private static final Upstream TIMEOUT =
            new Upstream(HttpStatus.GATEWAY_TIMEOUT, "UPSTREAM_TIMEOUT", "Upstream service did not respond in time");

    private final ProblemResponses problems;

    public UpstreamErrorResolver(ProblemResponses problems) {
        this.problems = problems;
    }

    @Override
    public ModelAndView resolveException(
            HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (response.isCommitted()) {
            return null;
        }
        Upstream upstream = classify(ex);
        if (upstream == null) {
            return null;
        }
        try {
            problems.write(response, upstream.status(), upstream.code(), upstream.detail());
        } catch (IOException e) {
            // Body could not be written (client gone). The failure is still handled, so do not rethrow.
        }
        return new ModelAndView();
    }

    private static Upstream classify(Throwable ex) {
        List<Throwable> chain = causeChain(ex);
        // Connect-type failures first: HttpConnectTimeoutException is also a timeout, so it must win here.
        if (chain.stream().anyMatch(UpstreamErrorResolver::isConnectFailure)) {
            return UNAVAILABLE;
        }
        if (chain.stream().anyMatch(UpstreamErrorResolver::isTimeout)) {
            return TIMEOUT;
        }
        if (chain.stream().anyMatch(t -> t instanceof ResourceAccessException)) {
            return UNAVAILABLE;
        }
        return null;
    }

    private static boolean isConnectFailure(Throwable t) {
        return t instanceof HttpConnectTimeoutException
                || t instanceof ConnectException
                || t instanceof UnknownHostException
                || t instanceof NoRouteToHostException;
    }

    private static boolean isTimeout(Throwable t) {
        return t instanceof SocketTimeoutException || t instanceof HttpTimeoutException;
    }

    /** The exception followed by its causes, stopping at a cycle or after {@link #MAX_CAUSE_DEPTH} levels. */
    private static List<Throwable> causeChain(Throwable ex) {
        List<Throwable> chain = new ArrayList<>();
        Throwable current = ex;
        while (current != null && chain.size() < MAX_CAUSE_DEPTH) {
            Throwable candidate = current;
            if (chain.stream().anyMatch(t -> t == candidate)) {
                break;
            }
            chain.add(candidate);
            current = candidate.getCause();
        }
        return chain;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private record Upstream(HttpStatus status, String code, String detail) {}
}
