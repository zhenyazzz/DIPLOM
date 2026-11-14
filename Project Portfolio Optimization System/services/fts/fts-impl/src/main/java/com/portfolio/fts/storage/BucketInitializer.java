package com.portfolio.fts.storage;

import com.portfolio.fts.config.FtsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BucketInitializer implements ApplicationRunner {

    private final ObjectStorage objectStorage;
    private final FtsProperties properties;

    @Override
    public void run(ApplicationArguments args) {
        for (FtsProperties.BucketPolicy policy : properties.buckets().values()) {
            String bucket = policy.name();
            objectStorage.ensureBucket(bucket);
            log.info("Ensured MinIO bucket '{}'", bucket);
        }
    }
}
