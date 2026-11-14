package com.portfolio.fts.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
public abstract class IntegrationTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Container
    protected static final MinIOContainer MINIO = new MinIOContainer(
            DockerImageName.parse("cgr.dev/chainguard/minio:latest-dev"
                            + "@sha256:5735e8125c4af7dd310dc5780508bfb850f0b68783a1a56089b5c64316a8f603")
                    .asCompatibleSubstituteFor("minio/minio"));

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("fts.minio.endpoint", MINIO::getS3URL);
        registry.add("fts.minio.public-endpoint", MINIO::getS3URL);
        registry.add("fts.minio.access-key", MINIO::getUserName);
        registry.add("fts.minio.secret-key", MINIO::getPassword);
    }

    @Autowired
    protected MockMvc mockMvc;

    /**
     * Every FTS MockMvc response is checked against {@code contracts/openapi/fts-v1.yaml}.
     * The outbound MinIO GET of a presigned URL is not an FTS endpoint and must not go through this helper.
     */
    protected ResultActions perform(RequestBuilder request) throws Exception {
        return mockMvc.perform(request).andExpect(OpenApiContract.validResponse());
    }
}
