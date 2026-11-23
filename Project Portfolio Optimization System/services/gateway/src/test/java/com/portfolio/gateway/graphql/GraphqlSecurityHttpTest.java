package com.portfolio.gateway.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.portfolio.gateway.security.IdentitySessionClient;
import com.portfolio.gateway.support.TestJwt;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GraphqlSecurityHttpTest {

    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static final String LEDGER_ID = "00000000-0000-4000-8000-000000000602";
    private static final String PROJECTS_QUERY = "{\"query\":\"query { projects { code } }\"}";
    private static final String ARCHIVE_MUTATION =
            "{\"query\":\"mutation($id: ID!) { archiveInitiative(id: $id) { code status } }\",\"variables\":{\"id\":\"%s\"}}"
                    .formatted(LEDGER_ID);

    @LocalServerPort
    int port;

    @MockitoBean
    IdentitySessionClient identitySessionClient;

    @BeforeEach
    void defaultSessionActive() {
        when(identitySessionClient.isSessionActive(anyString())).thenReturn(true);
    }

    @Test
    void noTokenReturnsUnauthorized() throws Exception {
        HttpResponse<String> response = post(PROJECTS_QUERY, null);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("UNAUTHORIZED").contains("Authentication required");
    }

    @Test
    void jwtWithoutActiveSessionReturnsUnauthorized() throws Exception {
        String token = TestJwt.roleToken(UUID.randomUUID(), "ADMIN");
        when(identitySessionClient.isSessionActive(token)).thenReturn(false);

        HttpResponse<String> response = post(PROJECTS_QUERY, token);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("UNAUTHORIZED").contains("Access session is not active");
    }

    @Test
    void jwtWithoutJtiReturnsUnauthorized() throws Exception {
        String token = TestJwt.tokenWithoutJti(UUID.randomUUID(), "ADMIN");

        HttpResponse<String> response = post(PROJECTS_QUERY, token);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("UNAUTHORIZED").contains("jti");
    }

    @Test
    void initiatorCannotArchiveReturnsForbidden() throws Exception {
        String token = TestJwt.roleToken(UUID.randomUUID(), "INITIATOR");

        HttpResponse<String> response = post(ARCHIVE_MUTATION, token);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("FORBIDDEN").contains("archiveInitiative");
    }

    @Test
    void initiatorCanReadProjects() throws Exception {
        String token = TestJwt.roleToken(UUID.randomUUID(), "INITIATOR");

        HttpResponse<String> response = post(PROJECTS_QUERY, token);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("PRJ-PORTAL").doesNotContain("errors");
    }

    @Test
    void adminCanArchive() throws Exception {
        String token = TestJwt.roleToken(UUID.randomUUID(), "ADMIN");

        HttpResponse<String> response = post(ARCHIVE_MUTATION, token);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("PRJ-LEDGER").contains("ARCHIVED").doesNotContain("errors");
    }

    private HttpResponse<String> post(String json, String bearer) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/graphql"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json));
        if (bearer != null) {
            builder.header("Authorization", "Bearer " + bearer);
        }
        return CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
}
