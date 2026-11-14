package com.portfolio.fts.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.portfolio.common.security.context.SecuritySupport;
import com.portfolio.fts.api.DownloadUrlResponse;
import com.portfolio.fts.api.FileBucket;
import com.portfolio.fts.api.FileResponse;
import com.portfolio.fts.config.FtsProperties;
import com.portfolio.fts.config.FtsProperties.BucketPolicy;
import com.portfolio.fts.config.FtsProperties.Minio;
import com.portfolio.fts.error.FtsException;
import com.portfolio.fts.storage.ObjectStorage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-01-15T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
    private static final Duration DOWNLOAD_TTL = Duration.ofMinutes(15);
    private static final byte[] CSV_BYTES = csv("hello");
    private static final byte[] PNG_MAGIC = {
        (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };
    private static final String CSV_SHA256 =
            "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824";
    private static final String PHYSICAL_IMPORTS = "fts-imports";

    @Mock
    FileRepository files;

    @Mock
    SecuritySupport security;

    InMemoryObjectStorage storage;
    FileService fileService;

    @BeforeEach
    void setUp() {
        storage = new InMemoryObjectStorage();
        FtsProperties properties = new FtsProperties(
                new Minio("http://minio", "http://public-minio", "access", "secret", "us-east-1"),
                DOWNLOAD_TTL,
                Map.of(
                        FileBucket.IMPORTS,
                        new BucketPolicy(PHYSICAL_IMPORTS, DataSize.ofBytes(10), Set.of("text/csv"))));
        fileService = new FileService(
                files, new FileMapperImpl(), storage, new ContentSignatureVerifier(), properties, security, CLOCK);
    }

    @Test
    @DisplayName("upload stores bytes in the policy bucket under a UUID key and returns size and SHA-256")
    void upload_storesBytesUnderUuidKeyAndReturnsSizeAndSha256() {
        // given
        stubSaveReturnsArgumentWithId();
        MultipartFile file = csvFile("report.csv", "text/csv", CSV_BYTES);

        // when
        FileResponse response = fileService.upload(FileBucket.IMPORTS, file);

        // then
        ArgumentCaptor<FileEntity> saved = ArgumentCaptor.forClass(FileEntity.class);
        verify(files).save(saved.capture());
        String objectKey = saved.getValue().getObjectKey();
        assertThat(UUID.fromString(objectKey)).isNotNull();
        assertThat(storage.storedIn(PHYSICAL_IMPORTS, objectKey)).isEqualTo(CSV_BYTES);

        assertThat(response.size()).isEqualTo(CSV_BYTES.length);
        assertThat(response.sha256()).isEqualTo(CSV_SHA256);
        assertThat(response.bucket()).isEqualTo(FileBucket.IMPORTS);
        assertThat(response.contentType()).isEqualTo("text/csv");
        assertThat(response.originalName()).isEqualTo("report.csv");
        assertThat(response.id()).isNotNull();
    }

    @Test
    @DisplayName("upload sets ownerId from SecuritySupport.requireUserId and exposes it on the response")
    void upload_setsOwnerIdFromCurrentUserAndExposesItOnResponse() {
        // given
        UUID ownerId = UUID.randomUUID();
        stubSaveReturnsArgumentWithId(ownerId);
        MultipartFile file = csvFile("report.csv", "text/csv", CSV_BYTES);

        // when
        FileResponse response = fileService.upload(FileBucket.IMPORTS, file);

        // then
        ArgumentCaptor<FileEntity> saved = ArgumentCaptor.forClass(FileEntity.class);
        verify(files).save(saved.capture());
        assertThat(saved.getValue().getOwnerId()).isEqualTo(ownerId);
        assertThat(response.ownerId()).isEqualTo(ownerId);
    }

    @Test
    @DisplayName("upload normalizes content type parameters and case to text/csv")
    void upload_normalizesContentTypeParametersAndCase() {
        // given
        stubSaveReturnsArgumentWithId();
        MultipartFile file = csvFile("report.csv", "TEXT/CSV; charset=UTF-8", CSV_BYTES);

        // when
        FileResponse response = fileService.upload(FileBucket.IMPORTS, file);

        // then
        assertThat(response.contentType()).isEqualTo("text/csv");
        ArgumentCaptor<FileEntity> saved = ArgumentCaptor.forClass(FileEntity.class);
        verify(files).save(saved.capture());
        assertThat(saved.getValue().getContentType()).isEqualTo("text/csv");
        assertThat(storage.contentTypeOf(PHYSICAL_IMPORTS, saved.getValue().getObjectKey()))
                .isEqualTo("text/csv");
    }

    @Test
    @DisplayName("empty file is rejected as EMPTY_FILE and nothing is stored")
    void upload_emptyFile_throwsEmptyFileAndStoresNothing() {
        // given
        MultipartFile file = csvFile("report.csv", "text/csv", new byte[0]);

        // when / then
        assertFtsError(() -> fileService.upload(FileBucket.IMPORTS, file), "EMPTY_FILE", HttpStatus.BAD_REQUEST);
        assertNothingStoredOrSaved();
    }

    @Test
    @DisplayName("disallowed content type is rejected as CONTENT_TYPE_NOT_ALLOWED and nothing is stored")
    void upload_disallowedContentType_throwsContentTypeNotAllowedAndStoresNothing() {
        // given
        MultipartFile file = csvFile("notes.txt", "text/plain", CSV_BYTES);

        // when / then
        assertFtsError(
                () -> fileService.upload(FileBucket.IMPORTS, file),
                "CONTENT_TYPE_NOT_ALLOWED",
                HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertNothingStoredOrSaved();
    }

    @Test
    @DisplayName("file larger than the bucket policy is rejected as FILE_TOO_LARGE and nothing is stored")
    void upload_tooLarge_throwsFileTooLargeAndStoresNothing() {
        // given
        MultipartFile file = csvFile("report.csv", "text/csv", new byte[11]);

        // when / then
        assertFtsError(
                () -> fileService.upload(FileBucket.IMPORTS, file),
                "FILE_TOO_LARGE",
                HttpStatus.CONTENT_TOO_LARGE);
        assertNothingStoredOrSaved();
    }

    @Test
    @DisplayName("PNG bytes declared as text/csv are rejected as CONTENT_MISMATCH and nothing is stored")
    void upload_pngBytesDeclaredAsCsv_throwsContentMismatchAndStoresNothing() {
        // given
        MultipartFile file = csvFile("report.csv", "text/csv", PNG_MAGIC);

        // when / then
        assertFtsError(
                () -> fileService.upload(FileBucket.IMPORTS, file),
                "CONTENT_MISMATCH",
                HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertNothingStoredOrSaved();
    }

    @Test
    @DisplayName("original filename path is stripped to the basename")
    void upload_stripsPathFromOriginalFilename() {
        // given
        stubSaveReturnsArgumentWithId();
        MultipartFile file = csvFile("C:\\x\\..\\report.csv", "text/csv", CSV_BYTES);

        // when
        FileResponse response = fileService.upload(FileBucket.IMPORTS, file);

        // then
        assertThat(response.originalName()).isEqualTo("report.csv");
    }

    @Test
    @DisplayName("blank original filename falls back to file")
    void upload_blankOriginalFilename_fallsBackToFile() {
        // given
        stubSaveReturnsArgumentWithId();
        MultipartFile file = csvFile("   ", "text/csv", CSV_BYTES);

        // when
        FileResponse response = fileService.upload(FileBucket.IMPORTS, file);

        // then
        assertThat(response.originalName()).isEqualTo("file");
    }

    @Test
    @DisplayName("repository save failure is rethrown and the uploaded object is deleted")
    void upload_saveFails_deletesStoredObjectAndRethrowsOriginal() {
        // given
        RuntimeException saveFailed = new IllegalStateException("db down");
        when(security.requireUserId()).thenReturn(UUID.randomUUID());
        when(files.save(any(FileEntity.class))).thenThrow(saveFailed);
        MultipartFile file = csvFile("report.csv", "text/csv", CSV_BYTES);

        // when / then
        assertThatThrownBy(() -> fileService.upload(FileBucket.IMPORTS, file)).isSameAs(saveFailed);
        assertThat(storage.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("save failure is rethrown with a storage delete failure in getSuppressed")
    void upload_saveFailsAndDeleteFails_rethrowsOriginalWithDeleteFailureSuppressed() {
        // given
        RuntimeException saveFailed = new IllegalStateException("db down");
        RuntimeException deleteFailed = new IllegalStateException("minio down");
        when(security.requireUserId()).thenReturn(UUID.randomUUID());
        when(files.save(any(FileEntity.class))).thenThrow(saveFailed);
        storage.failDeletesWith(deleteFailed);
        MultipartFile file = csvFile("report.csv", "text/csv", CSV_BYTES);

        // when / then
        assertThatThrownBy(() -> fileService.upload(FileBucket.IMPORTS, file))
                .isSameAs(saveFailed)
                .hasSuppressedException(deleteFailed);
    }

    @Test
    @DisplayName("IOException from MultipartFile.getInputStream is mapped to UPLOAD_READ_FAILED")
    void upload_inputStreamThrows_throwsUploadReadFailed() {
        // given
        MultipartFile file = new MockMultipartFile("file", "report.csv", "text/csv", CSV_BYTES) {
            @Override
            public InputStream getInputStream() throws IOException {
                throw new IOException("stream broken");
            }
        };

        // when / then
        assertFtsError(
                () -> fileService.upload(FileBucket.IMPORTS, file),
                "UPLOAD_READ_FAILED",
                HttpStatus.BAD_REQUEST);
        assertNothingStoredOrSaved();
    }

    @Test
    @DisplayName("getMetadata of a missing file throws FILE_NOT_FOUND")
    void getMetadata_missingFile_throwsFileNotFound() {
        // given
        UUID missingId = UUID.randomUUID();
        when(files.findByIdAndDeletedAtIsNull(missingId)).thenReturn(Optional.empty());

        // when / then
        assertFtsError(() -> fileService.getMetadata(missingId), "FILE_NOT_FOUND", HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("getMetadata of a deleted file throws FILE_NOT_FOUND")
    void getMetadata_deletedFile_throwsFileNotFound() {
        // given
        UUID deletedId = UUID.randomUUID();
        when(files.findByIdAndDeletedAtIsNull(deletedId)).thenReturn(Optional.empty());

        // when / then
        assertFtsError(() -> fileService.getMetadata(deletedId), "FILE_NOT_FOUND", HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("createDownloadUrl expires at fixed now plus configured TTL")
    void createDownloadUrl_expiresAtIsFixedNowPlusTtl() {
        // given
        FileEntity entity = activeFile(UUID.randomUUID(), UUID.randomUUID());
        when(files.findByIdAndDeletedAtIsNull(entity.getId())).thenReturn(Optional.of(entity));

        // when
        DownloadUrlResponse response = fileService.createDownloadUrl(entity.getId());

        // then
        assertThat(response.expiresAt()).isEqualTo(FIXED_NOW.plus(DOWNLOAD_TTL));
        assertThat(response.url())
                .isEqualTo(URI.create("https://files.example/" + PHYSICAL_IMPORTS + "/" + entity.getObjectKey()));
    }

    @Test
    @DisplayName("delete by owner sets deletedAt to the fixed clock instant")
    void delete_byOwner_setsDeletedAtToFixedNow() {
        // given
        UUID ownerId = UUID.randomUUID();
        FileEntity entity = activeFile(UUID.randomUUID(), ownerId);
        when(files.findByIdAndDeletedAtIsNull(entity.getId())).thenReturn(Optional.of(entity));
        when(security.requireUserId()).thenReturn(ownerId);

        // when
        fileService.delete(entity.getId());

        // then
        assertThat(entity.getDeletedAt()).isEqualTo(FIXED_NOW);
    }

    @Test
    @DisplayName("delete by ADMIN who is not the owner still marks the file deleted")
    void delete_byAdminNonOwner_setsDeletedAtToFixedNow() {
        // given
        UUID ownerId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        FileEntity entity = activeFile(UUID.randomUUID(), ownerId);
        when(files.findByIdAndDeletedAtIsNull(entity.getId())).thenReturn(Optional.of(entity));
        when(security.requireUserId()).thenReturn(adminId);
        when(security.requireRole()).thenReturn("ADMIN");

        // when
        fileService.delete(entity.getId());

        // then
        assertThat(entity.getDeletedAt()).isEqualTo(FIXED_NOW);
    }

    @Test
    @DisplayName("delete by another non-admin user is FORBIDDEN and deletedAt stays null")
    void delete_byOtherUser_throwsForbiddenAndLeavesDeletedAtNull() {
        // given
        UUID ownerId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        FileEntity entity = activeFile(UUID.randomUUID(), ownerId);
        when(files.findByIdAndDeletedAtIsNull(entity.getId())).thenReturn(Optional.of(entity));
        when(security.requireUserId()).thenReturn(otherId);
        when(security.requireRole()).thenReturn("USER");

        // when / then
        assertFtsError(() -> fileService.delete(entity.getId()), "FORBIDDEN", HttpStatus.FORBIDDEN);
        assertThat(entity.getDeletedAt()).isNull();
    }

    private void stubSaveReturnsArgumentWithId() {
        stubSaveReturnsArgumentWithId(UUID.randomUUID());
    }

    private void stubSaveReturnsArgumentWithId(UUID ownerId) {
        when(security.requireUserId()).thenReturn(ownerId);
        when(files.save(any(FileEntity.class))).thenAnswer(invocation -> {
            FileEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });
    }

    private FileEntity activeFile(UUID id, UUID ownerId) {
        FileEntity entity = new FileEntity();
        entity.setId(id);
        entity.setBucket(FileBucket.IMPORTS);
        entity.setObjectKey(UUID.randomUUID().toString());
        entity.setOriginalName("report.csv");
        entity.setContentType("text/csv");
        entity.setSizeBytes(CSV_BYTES.length);
        entity.setSha256(CSV_SHA256);
        entity.setOwnerId(ownerId);
        return entity;
    }

    private static byte[] csv(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    private static MockMultipartFile csvFile(String originalFilename, String contentType, byte[] content) {
        return new MockMultipartFile("file", originalFilename, contentType, content);
    }

    private void assertNothingStoredOrSaved() {
        verify(files, never()).save(any());
        assertThat(storage.isEmpty()).isTrue();
    }

    private static void assertFtsError(
            org.assertj.core.api.ThrowableAssert.ThrowingCallable action, String code, HttpStatus status) {
        assertThatThrownBy(action)
                .isInstanceOf(FtsException.class)
                .satisfies(ex -> {
                    FtsException error = (FtsException) ex;
                    assertThat(error.getCode()).isEqualTo(code);
                    assertThat(error.getStatus()).isEqualTo(status);
                });
    }

    static final class InMemoryObjectStorage implements ObjectStorage {

        private final Map<String, StoredObject> objects = new LinkedHashMap<>();
        private RuntimeException deleteFailure;

        boolean isEmpty() {
            return objects.isEmpty();
        }

        byte[] storedIn(String bucket, String objectKey) {
            StoredObject stored = objects.get(storageKey(bucket, objectKey));
            assertThat(stored).as("object %s/%s", bucket, objectKey).isNotNull();
            return stored.bytes();
        }

        String contentTypeOf(String bucket, String objectKey) {
            StoredObject stored = objects.get(storageKey(bucket, objectKey));
            assertThat(stored).as("object %s/%s", bucket, objectKey).isNotNull();
            return stored.contentType();
        }

        void failDeletesWith(RuntimeException failure) {
            this.deleteFailure = failure;
        }

        @Override
        public void ensureBucket(String bucket) {
            // no-op: unit tests do not require bucket provisioning
        }

        @Override
        public void put(String bucket, String objectKey, InputStream content, long size, String contentType) {
            try {
                objects.put(storageKey(bucket, objectKey), new StoredObject(content.readAllBytes(), contentType));
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }

        @Override
        public InputStream get(String bucket, String objectKey) {
            StoredObject stored = objects.get(storageKey(bucket, objectKey));
            if (stored == null) {
                throw new IllegalStateException("Missing object " + bucket + "/" + objectKey);
            }
            return new ByteArrayInputStream(stored.bytes());
        }

        @Override
        public void delete(String bucket, String objectKey) {
            if (deleteFailure != null) {
                throw deleteFailure;
            }
            objects.remove(storageKey(bucket, objectKey));
        }

        @Override
        public URI presignedDownloadUrl(String bucket, String objectKey, String downloadName, Duration ttl) {
            return URI.create("https://files.example/" + bucket + "/" + objectKey);
        }

        private static String storageKey(String bucket, String objectKey) {
            return bucket + "/" + objectKey;
        }

        private record StoredObject(byte[] bytes, String contentType) {}
    }
}
