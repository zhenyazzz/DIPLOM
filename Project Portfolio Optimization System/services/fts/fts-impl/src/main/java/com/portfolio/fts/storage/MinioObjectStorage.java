package com.portfolio.fts.storage;

import com.portfolio.fts.config.FtsProperties;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import org.springframework.http.ContentDisposition;
import org.springframework.stereotype.Component;

@Component
public class MinioObjectStorage implements ObjectStorage {

    private final MinioClient client;
    private final MinioClient presignClient;

    public MinioObjectStorage(FtsProperties properties) {
        FtsProperties.Minio minio = properties.minio();
        this.client = buildClient(minio.endpoint(), minio);
        this.presignClient = buildClient(minio.publicEndpoint(), minio);
    }

    @Override
    public void ensureBucket(String bucket) {
        call("ensureBucket", bucket, null, () -> {
            boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
            return null;
        });
    }

    @Override
    public void put(String bucket, String objectKey, InputStream content, long size, String contentType) {
        call("put", bucket, objectKey, () -> {
            client.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(content, size, -1)
                    .contentType(contentType)
                    .build());
            return null;
        });
    }

    @Override
    public InputStream get(String bucket, String objectKey) {
        return call("get", bucket, objectKey, () -> client.getObject(GetObjectArgs.builder()
                .bucket(bucket)
                .object(objectKey)
                .build()));
    }

    @Override
    public URI presignedDownloadUrl(String bucket, String objectKey, String downloadName, Duration ttl) {
        return call("presignedDownloadUrl", bucket, objectKey, () -> {
            String disposition = ContentDisposition.attachment()
                    .filename(downloadName, StandardCharsets.UTF_8)
                    .build()
                    .toString();
            String url = presignClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucket)
                    .object(objectKey)
                    .expiry(Math.toIntExact(ttl.toSeconds()))
                    .extraQueryParams(Map.of("response-content-disposition", disposition))
                    .build());
            return URI.create(url);
        });
    }

    private static MinioClient buildClient(String endpoint, FtsProperties.Minio minio) {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(minio.accessKey(), minio.secretKey())
                .region(minio.region())
                .build();
    }

    private <T> T call(String operation, String bucket, String objectKey, StorageCall<T> action) {
        try {
            return action.execute();
        } catch (Exception ex) {
            String target = objectKey == null ? bucket : bucket + "/" + objectKey;
            throw new StorageException(operation + " failed for " + target, ex);
        }
    }

    @FunctionalInterface
    private interface StorageCall<T> {
        T execute() throws Exception;
    }
}
