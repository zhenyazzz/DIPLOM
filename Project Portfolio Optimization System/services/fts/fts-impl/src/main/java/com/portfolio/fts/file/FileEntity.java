package com.portfolio.fts.file;

import com.portfolio.common.web.domain.BaseEntity;
import com.portfolio.fts.api.FileBucket;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "files")
public class FileEntity extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private FileBucket bucket;

    @Column(nullable = false, unique = true, length = 64)
    private String objectKey;

    @Column(nullable = false, length = 255)
    private String originalName;

    @Column(nullable = false, length = 255)
    private String contentType;

    @Column(nullable = false)
    private long sizeBytes;

    @Column(nullable = false, length = 64)
    private String sha256;

    private Instant deletedAt;

    public void markDeleted(Instant when) {
        this.deletedAt = when;
    }

    public boolean isOwnedBy(String userId) {
        return userId != null && userId.equals(getCreatedBy());
    }
}
