package com.portfolio.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.portfolio.gateway.support.TestJwt;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GatewayIntegrationTest {

    private static final String LOGIN_PATH = "/api/v1/auth/login";
    private static final String LOGIN_COOKIE = "refresh_token=abc; Path=/api/v1/auth; HttpOnly; SameSite=Lax";
    private static final String CORE_OK_BODY = "{\"ok\":true}";

    private static final AtomicReference<RecordedRequest> LAST = new AtomicReference<>();
    private static final AtomicBoolean CORE_RUNNING = new AtomicBoolean();
    private static final HttpServer CORE = startCore();
    private static final HttpClient CLIENT =
            HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();

    @LocalServerPort
    int port;

    @DynamicPropertySource
    static void coreRoutes(DynamicPropertyRegistry registry) {
        String coreUrl = "http://localhost:" + CORE.getAddress().getPort();
        registry.add("spring.cloud.gateway.server.webmvc.routes[0].uri", () -> coreUrl);
        registry.add("spring.cloud.gateway.server.webmvc.routes[1].uri", () -> coreUrl);
    }

    @BeforeEach
    void resetCore() {
        LAST.set(null);
    }

    @AfterAll
    static void stopCore() {
        if (CORE_RUNNING.getAndSet(false)) {
            CORE.stop(0);
        }
    }

    @Test
    @Order(1)
    void protectedApiWithoutTokenReturns401ProblemJsonAndDoesNotHitCore() throws Exception {
        HttpResponse<String> response = get("/api/v1/projects", Map.of());

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(contentType(response)).contains("application/problem+json");
        assertThat(response.body()).contains("\"code\":\"UNAUTHORIZED\"");
        assertThat(LAST.get()).isNull();
    }

    @Test
    @Order(2)
    void expiredTokenReturns401() throws Exception {
        String expired = TestJwt.expiredToken(UUID.randomUUID());

        HttpResponse<String> response = get("/api/v1/projects", Map.of("Authorization", "Bearer " + expired));

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(LAST.get()).isNull();
    }

    @Test
    @Order(3)
    void validTokenIsProxiedWithPathQueryAndAuthorizationUnchanged() throws Exception {
        String authorization = "Bearer " + TestJwt.userToken(UUID.randomUUID());

        HttpResponse<String> response = get("/api/v1/projects?page=2", Map.of("Authorization", authorization));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo(CORE_OK_BODY);

        RecordedRequest seen = LAST.get();
        assertThat(seen).isNotNull();
        assertThat(seen.method()).isEqualTo("GET");
        assertThat(seen.path()).isEqualTo("/api/v1/projects?page=2");
        assertThat(seen.header("Authorization")).isEqualTo(authorization);
    }

    @Test
    @Order(4)
    void loginIsPublicAndRefreshCookiePathIsPreserved() throws Exception {
        HttpResponse<String> response = postLogin("10.0.0.40");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Set-Cookie")).isPresent();
        assertThat(response.headers().firstValue("Set-Cookie").orElseThrow()).contains("Path=/api/v1/auth");
        assertThat(LAST.get()).isNotNull();
        assertThat(LAST.get().path()).isEqualTo(LOGIN_PATH);
    }

    @Test
    @Order(6)
    void loginRateLimitReturns429WithRetryAfterPerClientIp() throws Exception {
        String limitedIp = "10.0.0.60";

        for (int i = 1; i <= 3; i++) {
            HttpResponse<String> allowed = postLogin(limitedIp);
            assertThat(allowed.statusCode()).as("login attempt %d", i).isEqualTo(200);
        }

        HttpResponse<String> limited = postLogin(limitedIp);

        assertThat(limited.statusCode()).isEqualTo(429);
        assertThat(limited.headers().firstValue("Retry-After")).isPresent();
        assertThat(limited.body()).contains("TOO_MANY_REQUESTS");

        HttpResponse<String> otherClient = postLogin("10.0.0.61");

        assertThat(otherClient.statusCode()).isEqualTo(200);
    }

    @Test
    @Order(7)
    void internalFilesPathIsNotFoundEvenWithValidToken() throws Exception {
        String authorization = "Bearer " + TestJwt.userToken(UUID.randomUUID());

        HttpResponse<String> withToken = get("/api/v1/files/123", Map.of("Authorization", authorization));

        assertThat(withToken.statusCode()).isEqualTo(404);
        assertThat(withToken.body()).contains("NOT_FOUND");
        assertThat(LAST.get()).isNull();

        HttpResponse<String> withoutToken = get("/api/v1/files/123", Map.of());

        assertThat(withoutToken.statusCode()).isEqualTo(404);
        assertThat(withoutToken.body()).contains("NOT_FOUND");
        assertThat(LAST.get()).isNull();
    }

    @Test
    @Order(8)
    void corsPreflightIsAnsweredForAllowedOriginAndRejectedForOthers() throws Exception {
        HttpResponse<String> allowed = send(
                "OPTIONS",
                "/api/v1/projects",
                HttpRequest.BodyPublishers.noBody(),
                Map.of("Origin", "http://localhost:5173", "Access-Control-Request-Method", "GET"));

        assertThat(allowed.statusCode()).isEqualTo(200);
        assertThat(allowed.headers().firstValue("Access-Control-Allow-Origin")).hasValue("http://localhost:5173");
        assertThat(allowed.headers().firstValue("Access-Control-Allow-Credentials")).hasValue("true");

        HttpResponse<String> disallowed = send(
                "OPTIONS",
                "/api/v1/projects",
                HttpRequest.BodyPublishers.noBody(),
                Map.of("Origin", "http://evil.example", "Access-Control-Request-Method", "GET"));

        assertThat(disallowed.statusCode()).isEqualTo(403);
    }

    @Test
    @Order(9)
    void actuatorHealthIsPublic() throws Exception {
        HttpResponse<String> response = get("/actuator/health", Map.of());

        assertThat(response.statusCode()).isEqualTo(200);
    }

    @Test
    @Order(10)
    void coreUnavailableReturns502UpstreamUnavailable() throws Exception {
        CORE.stop(0);
        CORE_RUNNING.set(false);
        String authorization = "Bearer " + TestJwt.userToken(UUID.randomUUID());

        HttpResponse<String> response = get("/api/v1/projects", Map.of("Authorization", authorization));

        assertThat(response.statusCode()).isEqualTo(502);
        assertThat(response.body()).contains("UPSTREAM_UNAVAILABLE");
    }

    private HttpResponse<String> get(String path, Map<String, String> headers) throws IOException, InterruptedException {
        return send("GET", path, HttpRequest.BodyPublishers.noBody(), headers);
    }

    private HttpResponse<String> postLogin(String forwardedFor) throws IOException, InterruptedException {
        return send(
                "POST",
                LOGIN_PATH,
                HttpRequest.BodyPublishers.ofString("{}"),
                Map.of("Content-Type", "application/json", "X-Forwarded-For", forwardedFor));
    }

    private HttpResponse<String> send(
            String method, String path, HttpRequest.BodyPublisher body, Map<String, String> headers)
            throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .method(method, body);
        headers.forEach(builder::header);
        return CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String contentType(HttpResponse<String> response) {
        return response.headers().firstValue("Content-Type").orElse("");
    }

    private static HttpServer startCore() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
            server.createContext("/", GatewayIntegrationTest::handleCore);
            server.start();
            CORE_RUNNING.set(true);
            return server;
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to start fake core", ex);
        }
    }

    private static void handleCore(HttpExchange exchange) throws IOException {
        try {
            Map<String, String> headers = new LinkedHashMap<>();
            exchange.getRequestHeaders().forEach(
                    (name, values) -> headers.put(name.toLowerCase(Locale.ROOT), String.join(",", values)));
            URI uri = exchange.getRequestURI();
            String path = uri.getRawQuery() == null
                    ? uri.getRawPath()
                    : uri.getRawPath() + "?" + uri.getRawQuery();
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            LAST.set(new RecordedRequest(exchange.getRequestMethod(), path, headers, body));

            byte[] responseBody;
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            if (LOGIN_PATH.equals(uri.getRawPath())) {
                exchange.getResponseHeaders().add("Set-Cookie", LOGIN_COOKIE);
                responseBody = "{\"accessToken\":\"x\"}".getBytes(StandardCharsets.UTF_8);
            } else {
                responseBody = CORE_OK_BODY.getBytes(StandardCharsets.UTF_8);
            }
            exchange.sendResponseHeaders(200, responseBody.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(responseBody);
            }
        } finally {
            exchange.close();
        }
    }

    record RecordedRequest(String method, String path, Map<String, String> headers, String body) {

        String header(String name) {
            return headers.get(name.toLowerCase(Locale.ROOT));
        }
    }
}
