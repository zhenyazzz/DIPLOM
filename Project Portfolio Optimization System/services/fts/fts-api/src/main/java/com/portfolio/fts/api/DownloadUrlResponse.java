package com.portfolio.fts.api;

import java.net.URI;
import java.time.Instant;

public record DownloadUrlResponse(URI url, Instant expiresAt) {
}
