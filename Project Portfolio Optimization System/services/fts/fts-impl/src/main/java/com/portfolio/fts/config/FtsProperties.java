package com.portfolio.fts.config;

import com.portfolio.fts.api.FileBucket;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("fts")
public record FtsProperties(
        @Valid @NotNull Minio minio,
        @NotNull Duration downloadUrlTtl,
        @NotEmpty Map<FileBucket, @Valid BucketPolicy> buckets) {

    public BucketPolicy policyFor(FileBucket bucket) {
        BucketPolicy policy = buckets.get(bucket);
        if (policy == null) {
            throw new IllegalStateException("No policy configured for bucket " + bucket);
        }
        return policy;
    }

    /**
     * @param endpoint       address used by the service itself (inside the docker network)
     * @param publicEndpoint address presigned URLs are signed for (reachable by the browser)
     */
    public record Minio(
            @NotBlank String endpoint,
            @NotBlank String publicEndpoint,
            @NotBlank String accessKey,
            @NotBlank String secretKey,
            @NotBlank String region) {
    }

    public record BucketPolicy(
            @NotBlank String name,
            @NotNull DataSize maxSize,
            @NotEmpty Set<String> contentTypes) {

        public boolean allows(String contentType) {
            return contentTypes.contains(contentType);
        }
    }
}
