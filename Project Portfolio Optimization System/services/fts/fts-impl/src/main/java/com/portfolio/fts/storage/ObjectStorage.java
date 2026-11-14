package com.portfolio.fts.storage;

import java.io.InputStream;
import java.net.URI;
import java.time.Duration;

/**
 * Port to the binary store. {@code bucket} is the physical bucket name from {@code FtsProperties.BucketPolicy}.
 * Implementations wrap any store failure into {@link StorageException}.
 */
public interface ObjectStorage {

    void ensureBucket(String bucket);

    void put(String bucket, String objectKey, InputStream content, long size, String contentType);

    InputStream get(String bucket, String objectKey);

    URI presignedDownloadUrl(String bucket, String objectKey, String downloadName, Duration ttl);
}
