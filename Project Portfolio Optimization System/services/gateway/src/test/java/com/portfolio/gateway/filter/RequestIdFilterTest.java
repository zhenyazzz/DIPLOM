package com.portfolio.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestIdFilterTest {

    private static final String UUID_PATTERN =
            "[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}";

    private final RequestIdFilter filter = new RequestIdFilter();

    private record Run(HttpServletRequest seenByChain, String mdcDuringChain) {}

    private Run run(MockHttpServletRequest request, MockHttpServletResponse response) throws Exception {
        AtomicReference<HttpServletRequest> seen = new AtomicReference<>();
        AtomicReference<String> mdc = new AtomicReference<>();
        FilterChain chain = (req, res) -> {
            seen.set((HttpServletRequest) req);
            mdc.set(MDC.get(RequestIdFilter.MDC_KEY));
        };
        filter.doFilter(request, response, chain);
        return new Run(seen.get(), mdc.get());
    }

    @Test
    void generatesUuidWhenHeaderAbsent() throws Exception {
        var response = new MockHttpServletResponse();

        Run result = run(new MockHttpServletRequest(), response);

        String responseId = response.getHeader(RequestIdFilter.HEADER);
        assertThat(responseId).matches(UUID_PATTERN);
        assertThat(result.seenByChain().getHeader(RequestIdFilter.HEADER)).isEqualTo(responseId);
    }

    @Test
    void reusesValidIncomingId() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader(RequestIdFilter.HEADER, "abc-123.X:y_z");
        var response = new MockHttpServletResponse();

        Run result = run(request, response);

        assertThat(response.getHeader(RequestIdFilter.HEADER)).isEqualTo("abc-123.X:y_z");
        assertThat(result.seenByChain().getHeader(RequestIdFilter.HEADER)).isEqualTo("abc-123.X:y_z");
    }

    @Test
    void replacesTooLongIncomingId() throws Exception {
        String tooLong = "a".repeat(129);
        var request = new MockHttpServletRequest();
        request.addHeader(RequestIdFilter.HEADER, tooLong);
        var response = new MockHttpServletResponse();

        Run result = run(request, response);

        String responseId = response.getHeader(RequestIdFilter.HEADER);
        assertThat(responseId).matches(UUID_PATTERN).isNotEqualTo(tooLong);
        assertThat(result.seenByChain().getHeader(RequestIdFilter.HEADER)).isEqualTo(responseId);
    }

    @Test
    void replacesIncomingIdWithInvalidCharacters() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader(RequestIdFilter.HEADER, "bad id/with spaces");
        var response = new MockHttpServletResponse();

        Run result = run(request, response);

        String responseId = response.getHeader(RequestIdFilter.HEADER);
        assertThat(responseId).matches(UUID_PATTERN);
        assertThat(result.seenByChain().getHeader(RequestIdFilter.HEADER)).isEqualTo(responseId);
    }

    @Test
    void headerNameAppearsExactlyOnceAndReplacesInvalidValueCaseInsensitively() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("x-request-id", "bad value!");
        request.addHeader("Accept", "application/json");
        var response = new MockHttpServletResponse();

        Run result = run(request, response);

        HttpServletRequest seen = result.seenByChain();
        String responseId = response.getHeader(RequestIdFilter.HEADER);
        List<String> names = Collections.list(seen.getHeaderNames());
        assertThat(names.stream().filter(RequestIdFilter.HEADER::equalsIgnoreCase)).hasSize(1);
        assertThat(names).contains("Accept");
        assertThat(Collections.list(seen.getHeaders(RequestIdFilter.HEADER))).containsExactly(responseId);
        assertThat(seen.getHeader("x-request-id")).isEqualTo(responseId);
    }

    @Test
    void mdcIsSetDuringChainAndClearedAfterwards() throws Exception {
        var response = new MockHttpServletResponse();

        Run result = run(new MockHttpServletRequest(), response);

        assertThat(result.mdcDuringChain()).isEqualTo(response.getHeader(RequestIdFilter.HEADER));
        assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void mdcIsClearedWhenChainThrows() {
        var response = new MockHttpServletResponse();
        FilterChain failing = (req, res) -> {
            throw new IllegalStateException("boom");
        };

        assertThatThrownBy(() -> filter.doFilter(new MockHttpServletRequest(), response, failing))
                .isInstanceOf(IllegalStateException.class);
        assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
    }
}
