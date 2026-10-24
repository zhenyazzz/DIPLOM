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
class MethodSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void adminTokenCanAccessAdminEndpoint() throws Exception {
        String token = TestJwtFactory.validAdminToken(UUID.randomUUID());
        mockMvc.perform(get("/api/admin/secret").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scope").value("admin"));
    }

    @Test
    void userTokenOnAdminEndpointReturns403ProblemDetails() throws Exception {
        String token = TestJwtFactory.validUserToken(UUID.randomUUID());
        ProblemDetailsAsserts.assertForbidden(
                mockMvc.perform(get("/api/admin/secret").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)));
    }
}
