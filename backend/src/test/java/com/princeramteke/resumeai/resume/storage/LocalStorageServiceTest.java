package com.princeramteke.resumeai.resume.storage;

import com.princeramteke.resumeai.resume.exception.StorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalStorageServiceTest {

    @TempDir
    Path tempDir;

    private LocalStorageService storageService;

    @BeforeEach
    void setUp() {
        var config = new StorageConfig(tempDir.toString());
        storageService = new LocalStorageService(config);
    }

    @Test
    void store_savesFileAndReturnsPath() throws IOException {
        byte[] content = "resume content".getBytes();
        String path = storageService.store(1L, "resume.pdf", new ByteArrayInputStream(content));

        assertThat(path).isNotNull();
        assertThat(Files.exists(Path.of(path))).isTrue();
        assertThat(Files.readAllBytes(Path.of(path))).isEqualTo(content);
    }

    @Test
    void load_existingFile_returnsContent() throws IOException {
        byte[] content = "test data".getBytes();
        String path = storageService.store(1L, "test.pdf", new ByteArrayInputStream(content));

        try (InputStream is = storageService.load(path)) {
            assertThat(is.readAllBytes()).isEqualTo(content);
        }
    }

    @Test
    void load_missingFile_throwsStorageException() {
        assertThatThrownBy(() -> storageService.load("/nonexistent/file.pdf"))
                .isInstanceOf(StorageException.class);
    }

    @Test
    void delete_existingFile_removesIt() {
        byte[] content = "to delete".getBytes();
        String path = storageService.store(1L, "delete.pdf", new ByteArrayInputStream(content));

        storageService.delete(path);

        assertThat(Files.exists(Path.of(path))).isFalse();
    }

    @Test
    void exists_returnsCorrectly() {
        byte[] content = "check".getBytes();
        String path = storageService.store(1L, "check.pdf", new ByteArrayInputStream(content));

        assertThat(storageService.exists(path)).isTrue();
        assertThat(storageService.exists("/nonexistent")).isFalse();
    }
}
