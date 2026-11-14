package com.portfolio.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import com.portfolio.gateway.web.ProblemResponses;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

class InternalPathsFilterTest {

    private final InternalPathsFilter filter =
            new InternalPathsFilter(new ProblemResponses(JsonMapper.builder().build()));

    @Test
    void blocksFilesRootWith404ProblemDetails() throws Exception {
        assertBlocked("/api/v1/files");
    }

    @Test
    void blocksFilesSubPathWith404ProblemDetails() throws Exception {
        assertBlocked("/api/v1/files/123/content");
    }

    @Test
    void passesThroughSimilarlyNamedPrefix() throws Exception {
        assertPassed("/api/v1/filesystem");
    }

    @Test
    void passesThroughOtherApiPaths() throws Exception {
        assertPassed("/api/v1/projects");
    }

    private void assertBlocked(String uri) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(response.getContentType()).startsWith("application/problem+json");
        assertThat(response.getContentAsString()).contains("NOT_FOUND");
        assertThat(chain.getRequest()).isNull();
    }

    private void assertPassed(String uri) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
    }
}
