package com.princeramteke.resumeai.resume.storage;

import com.princeramteke.resumeai.resume.exception.StorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class LocalStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageService.class);

    private final Path basePath;

    public LocalStorageService(StorageConfig config) {
        this.basePath = Path.of(config.path());
        try {
            Files.createDirectories(basePath);
        } catch (IOException e) {
            throw new StorageException("Could not initialize storage directory: " + basePath, e);
        }
    }

    @Override
    public String store(Long userId, String filename, InputStream content) {
        String storedName = UUID.randomUUID() + "_" + sanitize(filename);
        Path userDir = basePath.resolve(String.valueOf(userId));
        try {
            Files.createDirectories(userDir);
            Path target = userDir.resolve(storedName);
            Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
            log.info("Stored file: userId={}, path={}", userId, target);
            return target.toString();
        } catch (IOException e) {
            throw new StorageException("Failed to store file: " + filename, e);
        }
    }

    @Override
    public InputStream load(String filePath) {
        Path path = Path.of(filePath);
        if (!Files.exists(path)) {
            throw new StorageException("File not found: " + filePath);
        }
        try {
            return Files.newInputStream(path);
        } catch (IOException e) {
            throw new StorageException("Failed to read file: " + filePath, e);
        }
    }

    @Override
    public void delete(String filePath) {
        try {
            Path path = Path.of(filePath);
            Files.deleteIfExists(path);
            log.info("Deleted file: {}", filePath);
        } catch (IOException e) {
            log.warn("Failed to delete file: {}", filePath, e);
        }
    }

    @Override
    public boolean exists(String filePath) {
        return Files.exists(Path.of(filePath));
    }

    private String sanitize(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
