package com.portfolio.fts.file;

import com.portfolio.common.security.context.SecuritySupport;
import com.portfolio.fts.api.DownloadUrlResponse;
import com.portfolio.fts.api.FileBucket;
import com.portfolio.fts.api.FileResponse;
import com.portfolio.fts.config.FtsProperties;
import com.portfolio.fts.config.FtsProperties.BucketPolicy;
import com.portfolio.fts.error.FtsException;
import com.portfolio.fts.storage.ObjectStorage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class FileService {

    private static final String FALLBACK_FILENAME = "file";
    private static final int MAX_ORIGINAL_NAME_LENGTH = 255;
    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";
    private static final String ADMIN_ROLE = "ADMIN";

    private final FileRepository files;
    private final FileMapper fileMapper;
    private final ObjectStorage objectStorage;
    private final FtsProperties properties;
    private final SecuritySupport security;
    private final Clock clock;

    @Transactional
    public FileResponse upload(FileBucket bucket, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw FtsException.emptyFile();
        }

        String contentType = normalizeContentType(file.getContentType());
        BucketPolicy policy = properties.policyFor(bucket);
        if (!policy.allows(contentType)) {
            throw FtsException.contentTypeNotAllowed(contentType, bucket);
        }
        if (file.getSize() > policy.maxSize().toBytes()) {
            throw FtsException.fileTooLarge(policy.maxSize());
        }

        String objectKey = UUID.randomUUID().toString();
        String sha256 = storeAndHash(policy.name(), objectKey, file, contentType);

        FileEntity entity = new FileEntity();
        entity.setBucket(bucket);
        entity.setObjectKey(objectKey);
        entity.setOriginalName(sanitizeOriginalName(file.getOriginalFilename()));
        entity.setContentType(contentType);
        entity.setSizeBytes(file.getSize());
        entity.setSha256(sha256);
        return fileMapper.toResponse(files.save(entity));
    }

    @Transactional(readOnly = true)
    public FileResponse getMetadata(UUID fileId) {
        return fileMapper.toResponse(requireActive(fileId));
    }

    @Transactional(readOnly = true)
    public FileContent openContent(UUID fileId) {
        FileEntity file = requireActive(fileId);
        InputStream stream = objectStorage.get(physicalName(file.getBucket()), file.getObjectKey());
        return new FileContent(fileMapper.toResponse(file), stream);
    }

    @Transactional(readOnly = true)
    public DownloadUrlResponse createDownloadUrl(UUID fileId) {
        FileEntity file = requireActive(fileId);
        Duration ttl = properties.downloadUrlTtl();
        URI url = objectStorage.presignedDownloadUrl(
                physicalName(file.getBucket()),
                file.getObjectKey(),
                file.getOriginalName(),
                ttl);
        return new DownloadUrlResponse(url, clock.instant().plus(ttl));
    }

    @Transactional
    public void delete(UUID fileId) {
        FileEntity file = requireActive(fileId);
        requireDeletePermission(file);
        file.markDeleted(clock.instant());
    }

    private FileEntity requireActive(UUID fileId) {
        return files.findByIdAndDeletedAtIsNull(fileId)
                .orElseThrow(() -> FtsException.fileNotFound(fileId));
    }

    private void requireDeletePermission(FileEntity file) {
        if (file.isOwnedBy(security.requireUserId().toString())) {
            return;
        }
        if (ADMIN_ROLE.equals(security.requireRole())) {
            return;
        }
        throw FtsException.forbidden();
    }

    private String physicalName(FileBucket bucket) {
        return properties.policyFor(bucket).name();
    }

    private String storeAndHash(String bucket, String objectKey, MultipartFile file, String contentType) {
        try (DigestInputStream digested = new DigestInputStream(file.getInputStream(), sha256Digest())) {
            objectStorage.put(bucket, objectKey, digested, file.getSize(), contentType);
            return HexFormat.of().formatHex(digested.getMessageDigest().digest());
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to read uploaded content", ex);
        }
    }

    private static MessageDigest sha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    private static String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return DEFAULT_CONTENT_TYPE;
        }
        int separator = contentType.indexOf(';');
        String mediaType = (separator >= 0 ? contentType.substring(0, separator) : contentType)
                .trim()
                .toLowerCase(Locale.ROOT);
        return mediaType.isEmpty() ? DEFAULT_CONTENT_TYPE : mediaType;
    }

    private static String sanitizeOriginalName(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return FALLBACK_FILENAME;
        }
        String name = originalFilename.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        name = name.trim();
        if (name.isEmpty()) {
            return FALLBACK_FILENAME;
        }
        return name.length() <= MAX_ORIGINAL_NAME_LENGTH ? name : name.substring(0, MAX_ORIGINAL_NAME_LENGTH);
    }
}
