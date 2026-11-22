package com.portfolio.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import com.portfolio.gateway.config.GatewayProperties;
import com.portfolio.gateway.support.MutableClock;
import com.portfolio.gateway.web.ProblemResponses;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

class LoginRateLimitFilterTest {

    private static final String LOGIN = LoginRateLimitFilter.LOGIN_PATH;

    private MutableClock clock;
    private LoginRateLimitFilter filter;

    @BeforeEach
    void setUp() {
        clock = new MutableClock();
        GatewayProperties properties = new GatewayProperties(
                new GatewayProperties.Cors(List.of("http://localhost:5173")),
                new GatewayProperties.LoginRateLimit(3, Duration.ofMinutes(1)),
                "http://localhost:8080");
        filter = new LoginRateLimitFilter(properties, new ProblemResponses(JsonMapper.builder().build()), clock);
    }

    @Test
    void allowsCapacityThenReturns429WithRetryAfter() throws Exception {
        for (int i = 0; i < 3; i++) {
            assertPassed(post(LOGIN, "10.0.0.1"));
        }

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = send(post(LOGIN, "10.0.0.1"), response);

        assertThat(chain.getRequest()).isNull();
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("60");
        assertThat(response.getContentAsString()).contains("TOO_MANY_REQUESTS");
    }

    @Test
    void allowsAgainAfterWindowElapses() throws Exception {
        for (int i = 0; i < 4; i++) {
            send(post(LOGIN, "10.0.0.2"), new MockHttpServletResponse());
        }

        clock.advance(Duration.ofSeconds(61));

        assertPassed(post(LOGIN, "10.0.0.2"));
    }

    @Test
    void limitsAreIndependentPerClientIp() throws Exception {
        for (int i = 0; i < 4; i++) {
            send(post(LOGIN, "10.0.0.3"), new MockHttpServletResponse());
        }

        assertPassed(post(LOGIN, "10.0.0.4"));
    }

    @Test
    void getOnLoginPathIsNeverLimited() throws Exception {
        for (int i = 0; i < 10; i++) {
            assertPassed(new MockHttpServletRequest("GET", LOGIN));
        }
    }

    @Test
    void postToOtherPathIsNeverLimited() throws Exception {
        for (int i = 0; i < 10; i++) {
            assertPassed(post("/api/v1/auth/refresh", "10.0.0.5"));
        }
    }

    @Test
    void retryAfterRoundsUpToWholeSeconds() throws Exception {
        for (int i = 0; i < 3; i++) {
            send(post(LOGIN, "10.0.0.6"), new MockHttpServletResponse());
        }

        clock.advance(Duration.ofMillis(30_500));

        MockHttpServletResponse response = new MockHttpServletResponse();
        send(post(LOGIN, "10.0.0.6"), response);

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("30");
    }

    private static MockHttpServletRequest post(String uri, String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", uri);
        request.setRemoteAddr(remoteAddr);
        return request;
    }

    private MockFilterChain send(MockHttpServletRequest request, MockHttpServletResponse response)
            throws Exception {
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);
        return chain;
    }

    private void assertPassed(MockHttpServletRequest request) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = send(request, response);
        assertThat(chain.getRequest()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
    }
}
