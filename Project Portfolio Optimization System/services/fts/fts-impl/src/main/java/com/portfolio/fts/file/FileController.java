package com.portfolio.fts.file;

import com.portfolio.fts.api.DownloadUrlResponse;
import com.portfolio.fts.api.FileBucket;
import com.portfolio.fts.api.FileResponse;
import com.portfolio.fts.api.FilesApi;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class FileController implements FilesApi {

    private final FileService files;

    @Override
    public ResponseEntity<FileResponse> upload(FileBucket bucket, MultipartFile file) {
        FileResponse body = files.upload(bucket, file);
        URI location = URI.create("/api/v1/files/" + body.id());
        return ResponseEntity.created(location).body(body);
    }

    @Override
    public ResponseEntity<FileResponse> getMetadata(UUID fileId) {
        return ResponseEntity.ok(files.getMetadata(fileId));
    }

    @Override
    public ResponseEntity<Resource> download(UUID fileId) {
        FileContent content = files.openContent(fileId);
        FileResponse metadata = content.metadata();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(metadata.contentType()))
                .contentLength(metadata.size())
                .headers(headers -> headers.setContentDisposition(
                        ContentDisposition.attachment()
                                .filename(metadata.originalName(), StandardCharsets.UTF_8)
                                .build()))
                .body(new InputStreamResource(content.stream()));
    }

    @Override
    public ResponseEntity<DownloadUrlResponse> createDownloadUrl(UUID fileId) {
        return ResponseEntity.ok(files.createDownloadUrl(fileId));
    }

    @Override
    public ResponseEntity<Void> delete(UUID fileId) {
        files.delete(fileId);
        return ResponseEntity.noContent().build();
    }
}
