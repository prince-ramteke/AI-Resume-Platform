package com.princeramteke.resumeai.resume.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage")
public record StorageConfig(String path) {

    public StorageConfig {
        if (path == null || path.isBlank()) {
            path = "./uploads";
        }
    }
}
