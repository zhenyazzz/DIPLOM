package com.portfolio.gateway.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.portfolio.gateway.catalog.StubDomainProjectCatalog;
import com.portfolio.gateway.security.IdentitySessionClient;
import com.portfolio.gateway.support.TestJwt;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PortfolioGraphqlHttpTest {

    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static final Pattern ID_FIELD = Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern STATUS_FIELD = Pattern.compile("\"status\"\\s*:\\s*\"([^\"]+)\"");
    private static final String PORTAL_ID = "00000000-0000-4000-8000-000000000601";
    private static final String LEDGER_ID = "00000000-0000-4000-8000-000000000602";
    private static final String OWNER_ID = "00000000-0000-4000-8000-000000000001";

    @LocalServerPort
    int port;

    @Autowired
    StubDomainProjectCatalog catalog;

    @MockitoBean
    IdentitySessionClient identitySessionClient;

    private String adminToken;

    @BeforeEach
    void resetCatalog() {
        catalog.reset();
        when(identitySessionClient.isSessionActive(anyString())).thenReturn(true);
        adminToken = TestJwt.roleToken(UUID.randomUUID(), "ADMIN");
    }

    @Test
    void projectCardBringsDepartmentGoalsAndInitiator() throws Exception {
        String body = """
                {"query":"query($id: ID!) { project(id: $id) { code status requestedBudget department { code } goals { level goal { code } } initiator { displayName } } }","variables":{"id":"%s"}}
                """.formatted(PORTAL_ID);

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

    @Test
    void submitInitiativeMovesLedgerToSubmitted() throws Exception {
        String body = """
                {"query":"mutation($id: ID!) { submitInitiative(id: $id) { code status } }","variables":{"id":"%s"}}
                """.formatted(LEDGER_ID);

        HttpResponse<String> response = post(body);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body())
                .contains("PRJ-LEDGER")
                .contains("SUBMITTED")
                .doesNotContain("errors");
    }

    @Test
    void submitAlreadySubmittedPortalIsIllegalTransition() throws Exception {
        String body = """
                {"query":"mutation($id: ID!) { submitInitiative(id: $id) { code status } }","variables":{"id":"%s"}}
                """.formatted(PORTAL_ID);

        HttpResponse<String> response = post(body);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("ILLEGAL_STATUS_TRANSITION");
    }

    @Test
    void registerInitiativeCreatesDraftAndAppearsInProjects() throws Exception {
        HttpResponse<String> registered = post("""
                {"query":"mutation($input: RegisterInitiativeInput!) { registerInitiative(input: $input) { code status } }","variables":{"input":{"code":"PRJ-NEW","title":"Новая инициатива","departmentCode":"IT","ownerId":"%s","requestedBudget":"100.0000","goals":[{"goalCode":"ALIGN","level":3}]}}}
                """.formatted(OWNER_ID));

        assertThat(registered.statusCode()).isEqualTo(200);
        assertThat(registered.body()).contains("PRJ-NEW").contains("DRAFT").doesNotContain("errors");

        HttpResponse<String> list = post("""
                {"query":"query { projects { code } }"}
                """);

        assertThat(list.statusCode()).isEqualTo(200);
        assertThat(list.body()).contains("PRJ-NEW").contains("PRJ-PORTAL").contains("PRJ-LEDGER").doesNotContain("errors");
    }

    @Test
    void projectsSortByRequestedBudgetDesc() throws Exception {
        String body = """
                {"query":"query { projects(sort: { field: REQUESTED_BUDGET, direction: DESC }) { code } }"}
                """;

        HttpResponse<String> response = post(body);

        assertThat(response.statusCode()).isEqualTo(200);
        String payload = response.body();
        assertThat(payload).doesNotContain("errors");
        assertThat(payload.indexOf("PRJ-PORTAL")).isGreaterThanOrEqualTo(0).isLessThan(payload.indexOf("PRJ-LEDGER"));
    }

    @Test
    void projectsFilterByDepartmentCodeFin() throws Exception {
        String body = """
                {"query":"query { projects(filter: { departmentCode: \\"FIN\\" }) { code } }"}
                """;

        HttpResponse<String> response = post(body);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("PRJ-LEDGER").doesNotContain("PRJ-PORTAL").doesNotContain("errors");
    }

    @Test
    void projectRegistryStatsCountsSeedProjects() throws Exception {
        String body = """
                {"query":"query { projectRegistryStats { totalCount totalRequestedBudget byStatus { status count } } }"}
                """;

        HttpResponse<String> response = post(body);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body())
                .contains("\"totalCount\":2")
                .contains("2100000.0000")
                .contains("DRAFT")
                .contains("SUBMITTED")
                .doesNotContain("errors");
    }

    @Test
    void registeredProjectFollowsHappyReviewChain() throws Exception {
        HttpResponse<String> registered = post("""
                {"query":"mutation($input: RegisterInitiativeInput!) { registerInitiative(input: $input) { id status } }","variables":{"input":{"code":"PRJ-CHAIN","title":"Цепочка согласования","departmentCode":"OPS","ownerId":"%s","requestedBudget":"50.0000","goals":[{"goalCode":"RISK","level":2}]}}}
                """.formatted(OWNER_ID));
        String id = field(registered, ID_FIELD);
        assertThat(field(registered, STATUS_FIELD)).isEqualTo("DRAFT");

        assertThat(mutateStatus("submitInitiative", id)).isEqualTo("SUBMITTED");
        assertThat(mutateStatus("startReview", id)).isEqualTo("UNDER_REVIEW");
        assertThat(mutateStatus("completeEvaluation", id)).isEqualTo("EVALUATED");
        assertThat(mutateStatus("approveForPortfolio", id)).isEqualTo("APPROVED_FOR_PORTFOLIO");
    }

    @Test
    void archiveInitiativeArchivesDraftLedger() throws Exception {
        String body = """
                {"query":"mutation($id: ID!) { archiveInitiative(id: $id) { code status } }","variables":{"id":"%s"}}
                """.formatted(LEDGER_ID);

        HttpResponse<String> response = post(body);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("PRJ-LEDGER").contains("ARCHIVED").doesNotContain("errors");

        HttpResponse<String> list = post("""
                {"query":"query { projects { code } }"}
                """);
        assertThat(list.body()).contains("PRJ-PORTAL").doesNotContain("PRJ-LEDGER").doesNotContain("errors");
    }

    @Test
    void rejectInitiativeRejectsSubmittedPortal() throws Exception {
        String body = """
                {"query":"mutation($id: ID!) { rejectInitiative(id: $id, reason: \\"out of scope\\") { code status } }","variables":{"id":"%s"}}
                """.formatted(PORTAL_ID);

        HttpResponse<String> response = post(body);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("PRJ-PORTAL").contains("REJECTED").doesNotContain("errors");
    }

    @Test
    void returnForRevisionReturnsSubmittedPortal() throws Exception {
        String body = """
                {"query":"mutation($id: ID!) { returnForRevision(id: $id, reason: \\"need details\\") { code status } }","variables":{"id":"%s"}}
                """.formatted(PORTAL_ID);

        HttpResponse<String> response = post(body);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("PRJ-PORTAL").contains("RETURNED_FOR_REVISION").doesNotContain("errors");
    }

    @Test
    void projectsFilterByGoalCodeAlign() throws Exception {
        String body = """
                {"query":"query { projects(filter: { goalCode: \\"ALIGN\\" }) { code } }"}
                """;

        HttpResponse<String> response = post(body);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("PRJ-PORTAL").doesNotContain("PRJ-LEDGER").doesNotContain("errors");
    }

    @Test
    void projectsFilterByMinBudgetKeepsPortalOnly() throws Exception {
        String body = """
                {"query":"query { projects(filter: { minBudget: \\"1000000.0000\\" }) { code requestedBudget } }"}
                """;

        HttpResponse<String> response = post(body);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("PRJ-PORTAL").doesNotContain("PRJ-LEDGER").doesNotContain("errors");
    }

    @Test
    void projectRegistryStatsIncludesBudgetByDepartment() throws Exception {
        String body = """
                {"query":"query { projectRegistryStats { budgetByDepartment { departmentCode totalRequestedBudget } } }"}
                """;

        HttpResponse<String> response = post(body);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body())
                .contains("IT")
                .contains("FIN")
                .contains("1200000.0000")
                .contains("900000.0000")
                .doesNotContain("errors");
    }

    @Test
    void registerDuplicateCodeIsConflict() throws Exception {
        String body = """
                {"query":"mutation($input: RegisterInitiativeInput!) { registerInitiative(input: $input) { code } }","variables":{"input":{"code":"PRJ-PORTAL","title":"Дубликат","departmentCode":"IT","ownerId":"%s","requestedBudget":"10.0000","goals":[{"goalCode":"ALIGN","level":1}]}}}
                """.formatted(OWNER_ID);

        HttpResponse<String> response = post(body);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("DUPLICATE_CODE");
    }

    private String mutateStatus(String mutation, String id) throws Exception {
        HttpResponse<String> response = post("""
                {"query":"mutation($id: ID!) { %s(id: $id) { status } }","variables":{"id":"%s"}}
                """.formatted(mutation, id));
        return field(response, STATUS_FIELD);
    }

    private static String field(HttpResponse<String> response, Pattern pattern) {
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).doesNotContain("errors");
        Matcher matcher = pattern.matcher(response.body());
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private HttpResponse<String> post(String json) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/graphql"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + adminToken)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
