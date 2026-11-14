package com.portfolio.fts.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.portfolio.fts.support.IntegrationTest;
import com.portfolio.fts.support.TestJwt;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class FilesApiIntegrationTest extends IntegrationTest {

    private static final byte[] CSV_BYTES = "id,name\n1,alpha\n".getBytes(StandardCharsets.UTF_8);
    private static final byte[] PNG_BYTES = {
        (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D
    };
    private static final String CSV_NAME = "data.csv";
    private static final String CSV_TYPE = "text/csv";

    @Autowired
    JsonMapper jsonMapper;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void uploadCsvToImports_returnsCreatedWithMetadataAndSha256() throws Exception {
        UUID userId = UUID.randomUUID();

        MvcResult result = upload(TestJwt.userToken(userId), "IMPORTS", csvFile(CSV_BYTES))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, containsString("/api/v1/files/")))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.bucket").value("IMPORTS"))
                .andExpect(jsonPath("$.originalName").value(CSV_NAME))
                .andExpect(jsonPath("$.contentType").value(CSV_TYPE))
                .andExpect(jsonPath("$.size").value(CSV_BYTES.length))
                .andExpect(jsonPath("$.sha256").value(sha256Hex(CSV_BYTES)))
                .andExpect(jsonPath("$.ownerId").value(userId.toString()))
                .andReturn();

        JsonNode body = readBody(result);
        assertThat(result.getResponse().getHeader(HttpHeaders.LOCATION))
                .endsWith("/api/v1/files/" + body.get("id").asString());
    }

    @Test
    void getMetadata_returnsSameDataAsUpload() throws Exception {
        UUID userId = UUID.randomUUID();
        JsonNode uploaded = uploadCsv(userId);

        perform(get("/api/v1/files/{id}", uploaded.get("id").asString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(TestJwt.userToken(userId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(uploaded.get("id").asString()))
                .andExpect(jsonPath("$.bucket").value("IMPORTS"))
                .andExpect(jsonPath("$.originalName").value(CSV_NAME))
                .andExpect(jsonPath("$.contentType").value(CSV_TYPE))
                .andExpect(jsonPath("$.size").value(CSV_BYTES.length))
                .andExpect(jsonPath("$.sha256").value(sha256Hex(CSV_BYTES)))
                .andExpect(jsonPath("$.ownerId").value(userId.toString()));
    }

    @Test
    void getContent_returnsExactBytesWithCsvHeaders() throws Exception {
        UUID userId = UUID.randomUUID();
        JsonNode uploaded = uploadCsv(userId);

        byte[] body = perform(get("/api/v1/files/{id}/content", uploaded.get("id").asString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(TestJwt.userToken(userId))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.parseMediaType(CSV_TYPE)))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString(CSV_NAME)))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("attachment")))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        assertThat(body).isEqualTo(CSV_BYTES);
    }

    @Test
    void createDownloadUrl_presignedUrlServesSameBytes() throws Exception {
        UUID userId = UUID.randomUUID();
        JsonNode uploaded = uploadCsv(userId);

        MvcResult result = perform(post("/api/v1/files/{id}/download-url", uploaded.get("id").asString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(TestJwt.userToken(userId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").isNotEmpty())
                .andExpect(jsonPath("$.expiresAt").isNotEmpty())
                .andReturn();

        String url = readBody(result).get("url").asString();
        assertThat(url).startsWith(MINIO.getS3URL());

        try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()) {
            HttpResponse<byte[]> response = client.send(
                    HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10)).GET().build(),
                    HttpResponse.BodyHandlers.ofByteArray());
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).isEqualTo(CSV_BYTES);
        }
    }

    @Test
    void deleteByOwner_thenGetReturnsFileNotFound() throws Exception {
        UUID ownerId = UUID.randomUUID();
        String ownerToken = TestJwt.userToken(ownerId);
        JsonNode uploaded = uploadCsv(ownerId);
        String fileId = uploaded.get("id").asString();

        perform(delete("/api/v1/files/{id}", fileId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isNoContent());

        perform(get("/api/v1/files/{id}", fileId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FILE_NOT_FOUND"));
    }

    @Test
    void deleteByAnotherUser_returnsForbidden_adminCanDelete() throws Exception {
        UUID ownerId = UUID.randomUUID();
        String fileId = uploadCsv(ownerId).get("id").asString();

        perform(delete("/api/v1/files/{id}", fileId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(TestJwt.userToken(UUID.randomUUID()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        perform(delete("/api/v1/files/{id}", fileId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(TestJwt.adminToken(UUID.randomUUID()))))
                .andExpect(status().isNoContent());
    }

    @Test
    void uploadDisallowedType_returns415ContentTypeNotAllowed() throws Exception {
        upload(TestJwt.userToken(UUID.randomUUID()), "IMPORTS", pngFile("image/png"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("CONTENT_TYPE_NOT_ALLOWED"));
    }

    @Test
    void uploadEmptyFile_returns400EmptyFile() throws Exception {
        upload(TestJwt.userToken(UUID.randomUUID()), "IMPORTS", csvFile(new byte[0]))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EMPTY_FILE"));
    }

    @Test
    void uploadUnknownBucket_returns400BadRequest() throws Exception {
        upload(TestJwt.userToken(UUID.randomUUID()), "NOT_A_BUCKET", csvFile(CSV_BYTES))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void uploadMissingFilePart_returns400BadRequest() throws Exception {
        upload(TestJwt.userToken(UUID.randomUUID()), "IMPORTS", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void uploadPngBytesAsCsv_returns415ContentMismatch() throws Exception {
        upload(TestJwt.userToken(UUID.randomUUID()), "IMPORTS", pngFile(CSV_TYPE))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("CONTENT_MISMATCH"));
    }

    @Test
    void requestWithoutToken_returnsUnauthorized() throws Exception {
        perform(get("/api/v1/files/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void flywayCreatedFilesTableInFtsSchema() {
        Integer tables = jdbcTemplate.queryForObject(
                """
                select count(*)
                from information_schema.tables
                where table_schema = 'fts' and table_name = 'files'
                """,
                Integer.class);
        assertThat(tables).isEqualTo(1);
    }

    private JsonNode uploadCsv(UUID userId) throws Exception {
        MvcResult result = upload(TestJwt.userToken(userId), "IMPORTS", csvFile(CSV_BYTES))
                .andExpect(status().isCreated())
                .andReturn();
        return readBody(result);
    }

    private ResultActions upload(String token, String bucket, MockMultipartFile file) throws Exception {
        var request = multipart("/api/v1/files").param("bucket", bucket);
        if (file != null) {
            request.file(file);
        }
        if (token != null) {
            request.header(HttpHeaders.AUTHORIZATION, bearer(token));
        }
        return perform(request);
    }

    private JsonNode readBody(MvcResult result) throws Exception {
        return jsonMapper.readTree(result.getResponse().getContentAsString());
    }

    private static MockMultipartFile csvFile(byte[] content) {
        return new MockMultipartFile("file", CSV_NAME, CSV_TYPE, content);
    }

    private static MockMultipartFile pngFile(String contentType) {
        return new MockMultipartFile("file", "pic.png", contentType, PNG_BYTES);
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static String sha256Hex(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
