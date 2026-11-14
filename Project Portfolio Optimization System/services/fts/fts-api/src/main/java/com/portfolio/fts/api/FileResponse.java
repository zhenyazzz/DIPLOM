package com.portfolio.fts.api;

import java.time.Instant;
import java.util.UUID;

public record FileResponse(
        UUID id,
        FileBucket bucket,
        String originalName,
        String contentType,
        long size,
        String sha256,
        String createdBy,
        Instant createdAt) {
}
