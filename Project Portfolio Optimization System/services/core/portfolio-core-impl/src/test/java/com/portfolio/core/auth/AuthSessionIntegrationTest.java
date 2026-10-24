package com.portfolio.core.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.HttpHeaders.SET_COOKIE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.portfolio.core.support.PostgresIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class AuthSessionIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JsonMapper jsonMapper;

    @Test
    void loginRefreshLogout_sessionLifecycle() throws Exception {
        String email = "session-user@example.com";
        String password = "password123";

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "displayName": "Session User",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("INITIATOR"));

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(header().string(SET_COOKIE, containsString("refresh_token=")))
                .andExpect(header().string(SET_COOKIE, containsString("HttpOnly")))
                .andExpect(header().string(SET_COOKIE, containsString("Path=/api/v1/auth")))
                .andExpect(header().string(SET_COOKIE, containsString("SameSite=Lax")))
                .andReturn();

        Cookie firstRefresh = loginResult.getResponse().getCookie(RefreshCookieService.COOKIE_NAME);
        assertThat(firstRefresh).isNotNull();
        assertThat(firstRefresh.isHttpOnly()).isTrue();
        String firstRaw = firstRefresh.getValue();

        JsonNode loginBody = jsonMapper.readTree(loginResult.getResponse().getContentAsString());
        String accessToken = loginBody.get("accessToken").asString();
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("INITIATOR"));

        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(firstRefresh))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(header().string(SET_COOKIE, containsString("refresh_token=")))
                .andReturn();

        Cookie rotated = refreshResult.getResponse().getCookie(RefreshCookieService.COOKIE_NAME);
        assertThat(rotated).isNotNull();
        assertThat(rotated.getValue()).isNotEqualTo(firstRaw);

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(firstRefresh))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/logout").cookie(rotated))
                .andExpect(status().isNoContent())
                .andExpect(header().string(SET_COOKIE, containsString("Max-Age=0")));

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(rotated))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_uniformUnauthorizedForBadPassword() throws Exception {
        String email = "uniform@example.com";
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "displayName": "Uniform",
                                  "password": "password123"
                                }
                                """.formatted(email)))
                .andExpect(status().isCreated());

        MvcResult badPassword = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "wrong-password"
                                }
                                """.formatted(email)))
                .andExpect(status().isUnauthorized())
                .andReturn();

        MvcResult unknownEmail = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "missing@example.com",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andReturn();

        JsonNode badBody = jsonMapper.readTree(badPassword.getResponse().getContentAsString());
        JsonNode missingBody = jsonMapper.readTree(unknownEmail.getResponse().getContentAsString());
        assertThat(badBody.get("code").asText()).isEqualTo(missingBody.get("code").asText());
        assertThat(badBody.get("message").asText()).isEqualTo(missingBody.get("message").asText());
    }
}
