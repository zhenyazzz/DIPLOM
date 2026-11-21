package com.portfolio.gateway.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PortfolioGraphqlHttpTest {

    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    @LocalServerPort
    int port;

    @Test
    void projectCardBringsDepartmentGoalsAndInitiator() throws Exception {
        String body = """
                {"query":"query($id: ID!) { project(id: $id) { code status requestedBudget department { code } goals { level goal { code } } initiator { displayName } } }","variables":{"id":"00000000-0000-4000-8000-000000000601"}}
                """;

        HttpResponse<String> response = post(body);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body())
                .contains("PRJ-PORTAL")
                .contains("SUBMITTED")
                .contains("1200000.0000")
                .contains("IT")
                .contains("ALIGN")
                .contains("VALUE")
                .contains("System Admin")
                .doesNotContain("errors");
    }

    @Test
    void projectsFilterKeepsOnlyDrafts() throws Exception {
        String body = """
                {"query":"query { projects(filter: { status: DRAFT }) { code } }"}
                """;

        HttpResponse<String> response = post(body);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("PRJ-LEDGER").doesNotContain("PRJ-PORTAL").doesNotContain("errors");
    }

    @Test
    void missingProjectIsNotFound() throws Exception {
        String body = """
                {"query":"query { project(id: \\"00000000-0000-4000-8000-000000000699\\") { code } }"}
                """;

        HttpResponse<String> response = post(body);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("NOT_FOUND").contains("Project not found");
    }

    @Test
    void malformedIdIsBadRequest() throws Exception {
        String body = """
                {"query":"query { project(id: \\"nope\\") { code } }"}
                """;

        HttpResponse<String> response = post(body);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("BAD_REQUEST").contains("Invalid project id");
    }

    private HttpResponse<String> post(String json) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/graphql"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
