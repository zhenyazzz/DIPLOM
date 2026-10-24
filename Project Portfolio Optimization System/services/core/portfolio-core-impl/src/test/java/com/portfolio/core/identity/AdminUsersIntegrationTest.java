package com.portfolio.core.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.SET_COOKIE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.portfolio.core.api.identity.UserRole;
import com.portfolio.core.auth.RefreshCookieService;
import com.portfolio.core.support.PostgresIntegrationTest;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class AdminUsersIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JsonMapper jsonMapper;

    @Autowired
    UserRepository userRepository;

    @Test
    void adminCanListUsers_initiatorForbidden() throws Exception {
        String adminEmail = "admin-list@example.com";
        String initiatorEmail = "initiator-list@example.com";
        String password = "password123";

        register(adminEmail, "Admin List", password);
        promoteToAdmin(adminEmail);
        register(initiatorEmail, "Initiator List", password);

        String adminToken = loginAccessToken(adminEmail, password);
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.email=='%s')]".formatted(adminEmail)).exists())
                .andExpect(jsonPath("$[?(@.email=='%s')]".formatted(initiatorEmail)).exists());

        String initiatorToken = loginAccessToken(initiatorEmail, password);
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + initiatorToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void blockUser_revokesRefreshAndDeniesLogin() throws Exception {
        String adminEmail = "admin-block@example.com";
        String targetEmail = "target-block@example.com";
        String password = "password123";

        register(adminEmail, "Admin Block", password);
        promoteToAdmin(adminEmail);
        register(targetEmail, "Target Block", password);

        MvcResult targetLogin = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(targetEmail, password)))
                .andExpect(status().isOk())
                .andReturn();

        Cookie refreshCookie = targetLogin.getResponse().getCookie(RefreshCookieService.COOKIE_NAME);
        assertThat(refreshCookie).isNotNull();

        UUID targetId = userRepository.findByEmailIgnoreCase(targetEmail).orElseThrow().getId();
        String adminToken = loginAccessToken(adminEmail, password);

        mockMvc.perform(put("/api/v1/admin/users/{userId}/blocked", targetId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "blocked": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blocked").value(true));

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(refreshCookie))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(targetEmail, password)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void adminCanChangeRole() throws Exception {
        String adminEmail = "admin-role@example.com";
        String targetEmail = "target-role@example.com";
        String password = "password123";

        register(adminEmail, "Admin Role", password);
        promoteToAdmin(adminEmail);
        register(targetEmail, "Target Role", password);

        UUID targetId = userRepository.findByEmailIgnoreCase(targetEmail).orElseThrow().getId();
        String adminToken = loginAccessToken(adminEmail, password);

        mockMvc.perform(put("/api/v1/admin/users/{userId}/role", targetId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "role": "EXPERT_ANALYST"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("EXPERT_ANALYST"));

        String expertToken = loginAccessToken(targetEmail, password);
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + expertToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("EXPERT_ANALYST"));
    }

    private void register(String email, String displayName, String password) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "displayName": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, displayName, password)))
                .andExpect(status().isCreated());
    }

    private void promoteToAdmin(String email) {
        UserEntity user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        user.setRole(UserRole.ADMIN);
        userRepository.save(user);
    }

    private String loginAccessToken(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = jsonMapper.readTree(result.getResponse().getContentAsString());
        return body.get("accessToken").asString();
    }
}
