package com.portfolio.fts.support;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.mockmvc.MockMvcRequest;
import com.atlassian.oai.validator.mockmvc.MockMvcResponse;
import com.atlassian.oai.validator.mockmvc.OpenApiMatchers.OpenApiValidationException;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.report.ValidationReport;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.test.web.servlet.ResultMatcher;

/**
 * Loads {@code contracts/openapi/fts-v1.yaml} and validates MockMvc <em>responses</em> against it.
 * Request validation is skipped: multipart MockMvc bodies are a known limitation of the library.
 */
public final class OpenApiContract {

    private static final OpenApiInteractionValidator VALIDATOR = createValidator();

    private OpenApiContract() {}

    public static ResultMatcher validResponse() {
        return result -> {
            Request request = MockMvcRequest.of(result.getRequest());
            ValidationReport report = VALIDATOR.validateResponse(
                    request.getPath(), request.getMethod(), MockMvcResponse.of(result.getResponse()));
            if (report.hasErrors()) {
                throw new OpenApiValidationException(report);
            }
        };
    }

    static Path specPath() {
        Path dir = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        while (dir != null) {
            Path candidate = dir.resolve("contracts").resolve("openapi").resolve("fts-v1.yaml");
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            dir = dir.getParent();
        }
        throw new IllegalStateException(
                "Could not find contracts/openapi/fts-v1.yaml starting from " + System.getProperty("user.dir"));
    }

    private static OpenApiInteractionValidator createValidator() {
        Path spec = specPath();
        try {
            return OpenApiInteractionValidator.createForInlineApiSpecification(Files.readString(spec))
                    .build();
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to read OpenAPI spec at " + spec, ex);
        }
    }
}
