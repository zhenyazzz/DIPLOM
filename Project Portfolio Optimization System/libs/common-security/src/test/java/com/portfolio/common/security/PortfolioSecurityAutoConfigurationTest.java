package com.portfolio.common.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.portfolio.common.security.support.ProblemDetailsAsserts;
import com.portfolio.common.security.support.TestApplication;
import com.portfolio.common.security.support.TestJwtFactory;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = TestApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PortfolioSecurityAutoConfigurationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void validTokenOnProtectedEndpointReturns200() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = TestJwtFactory.validUserToken(userId);

        mockMvc.perform(get("/api/protected/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void missingTokenOnProtectedEndpointReturns401ProblemDetails() throws Exception {
        ProblemDetailsAsserts.assertUnauthorized(mockMvc.perform(get("/api/protected/me")));
    }

    @Test
    void expiredTokenReturns401ProblemDetails() throws Exception {
        String token = TestJwtFactory.expiredToken(UUID.randomUUID());
        ProblemDetailsAsserts.assertUnauthorized(
                mockMvc.perform(get("/api/protected/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)));
    }

    @Test
    void wrongSecretTokenReturns401ProblemDetails() throws Exception {
        String token = TestJwtFactory.wrongSecretToken(UUID.randomUUID());
        ProblemDetailsAsserts.assertUnauthorized(
                mockMvc.perform(get("/api/protected/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)));
    }

    @Test
    void permitAllWithoutTokenReturns200() throws Exception {
        mockMvc.perform(get("/api/public/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }
}
