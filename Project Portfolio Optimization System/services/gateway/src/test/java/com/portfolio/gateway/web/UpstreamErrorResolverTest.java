package com.portfolio.gateway.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.http.HttpConnectTimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.servlet.ModelAndView;
import tools.jackson.databind.json.JsonMapper;

class UpstreamErrorResolverTest {

    private final UpstreamErrorResolver resolver =
            new UpstreamErrorResolver(new ProblemResponses(JsonMapper.builder().build()));

    @Test
    void resourceAccessExceptionWrappingConnectExceptionIsBadGatewayUnavailable() throws Exception {
        var ex = new ResourceAccessException("I/O error", new ConnectException("Connection refused"));
        var response = new MockHttpServletResponse();

        ModelAndView mav = resolver.resolveException(new MockHttpServletRequest(), response, null, ex);

        assertThat(mav).isNotNull();
        assertThat(mav.isEmpty()).isTrue();
        assertThat(response.getStatus()).isEqualTo(502);
        assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat(response.getContentAsString()).contains("UPSTREAM_UNAVAILABLE");
    }

    @Test
    void resourceAccessExceptionWrappingSocketTimeoutIsGatewayTimeout() throws Exception {
        var ex = new ResourceAccessException("I/O error", new SocketTimeoutException("Read timed out"));
        var response = new MockHttpServletResponse();

        ModelAndView mav = resolver.resolveException(new MockHttpServletRequest(), response, null, ex);

        assertThat(mav).isNotNull();
        assertThat(response.getStatus()).isEqualTo(504);
        assertThat(response.getContentAsString()).contains("UPSTREAM_TIMEOUT");
    }

    @Test
    void httpConnectTimeoutIsBadGatewayUnavailableNotTimeout() throws Exception {
        var ex = new HttpConnectTimeoutException("connect timed out");
        var response = new MockHttpServletResponse();

        ModelAndView mav = resolver.resolveException(new MockHttpServletRequest(), response, null, ex);

        assertThat(mav).isNotNull();
        assertThat(response.getStatus()).isEqualTo(502);
        assertThat(response.getContentAsString()).contains("UPSTREAM_UNAVAILABLE");
    }

    @Test
    void unrelatedExceptionIsNotHandled() throws Exception {
        var response = new MockHttpServletResponse();

        ModelAndView mav = resolver.resolveException(
                new MockHttpServletRequest(), response, null, new IllegalStateException("boom"));

        assertThat(mav).isNull();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsString()).isEmpty();
    }

    @Test
    void committedResponseIsNotHandled() throws Exception {
        var response = new MockHttpServletResponse();
        response.flushBuffer();

        ModelAndView mav = resolver.resolveException(
                new MockHttpServletRequest(), response, null, new ConnectException("Connection refused"));

        assertThat(mav).isNull();
        assertThat(response.getContentAsString()).isEmpty();
    }
}
