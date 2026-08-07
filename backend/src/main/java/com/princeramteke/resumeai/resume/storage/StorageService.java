package com.princeramteke.resumeai.resume.storage;

import java.io.InputStream;

public interface StorageService {

    String store(Long userId, String filename, InputStream content);

    InputStream load(String filePath);

    void delete(String filePath);

    boolean exists(String filePath);
}
