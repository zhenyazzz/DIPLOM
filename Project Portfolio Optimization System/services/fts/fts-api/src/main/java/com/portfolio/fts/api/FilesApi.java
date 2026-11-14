package com.portfolio.fts.api;

import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

/**
 * File transfer contract ({@code /api/v1/files/**}). Files are immutable; a new version is a new id.
 */
@RequestMapping("/api/v1/files")
public interface FilesApi {

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<FileResponse> upload(
            @RequestParam FileBucket bucket,
            @RequestPart MultipartFile file);

    @GetMapping("/{fileId}")
    ResponseEntity<FileResponse> getMetadata(@PathVariable UUID fileId);

    @GetMapping("/{fileId}/content")
    ResponseEntity<Resource> download(@PathVariable UUID fileId);

    @PostMapping("/{fileId}/download-url")
    ResponseEntity<DownloadUrlResponse> createDownloadUrl(@PathVariable UUID fileId);

    @DeleteMapping("/{fileId}")
    ResponseEntity<Void> delete(@PathVariable UUID fileId);
}
