package com.portfolio.fts.error;

import com.portfolio.fts.api.FileBucket;
import java.util.UUID;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.util.unit.DataSize;

@Getter
public class FtsException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    private FtsException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public static FtsException fileNotFound(UUID id) {
        return new FtsException(HttpStatus.NOT_FOUND, "FILE_NOT_FOUND", "File not found: " + id);
    }

    public static FtsException emptyFile() {
        return new FtsException(HttpStatus.BAD_REQUEST, "EMPTY_FILE", "Uploaded file is empty");
    }

    public static FtsException contentTypeNotAllowed(String contentType, FileBucket bucket) {
        return new FtsException(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "CONTENT_TYPE_NOT_ALLOWED",
                "Content type '%s' is not allowed in bucket %s".formatted(contentType, bucket));
    }

    public static FtsException fileTooLarge(DataSize maxSize) {
        return new FtsException(
                HttpStatus.CONTENT_TOO_LARGE,
                "FILE_TOO_LARGE",
                "File exceeds the maximum size of %s".formatted(maxSize));
    }

    public static FtsException forbidden() {
        return new FtsException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Not allowed to delete this file");
    }
}
