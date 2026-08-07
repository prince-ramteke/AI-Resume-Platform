package com.princeramteke.resumeai.resume.validation;

import com.princeramteke.resumeai.resume.exception.InvalidResumeException;
import com.princeramteke.resumeai.resume.exception.ResumeTooLargeException;
import com.princeramteke.resumeai.resume.exception.UnsupportedFileTypeException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

@Component
public class FileValidator {

    private static final long MAX_SIZE_BYTES = 10L * 1024 * 1024;

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "docx");

    // PDF: %PDF (0x25504446)
    private static final byte[] PDF_MAGIC = {0x25, 0x50, 0x44, 0x46};
    // DOCX (ZIP): PK (0x504B0304)
    private static final byte[] ZIP_MAGIC = {0x50, 0x4B, 0x03, 0x04};

    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidResumeException("File is empty or missing");
        }

        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new ResumeTooLargeException();
        }

        validateExtension(file.getOriginalFilename());
        validateContentType(file.getContentType());
        validateMagicBytes(file);
    }

    private void validateExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new UnsupportedFileTypeException("File must be PDF or DOCX");
        }
        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new UnsupportedFileTypeException("File must be PDF or DOCX, got: ." + extension);
        }
    }

    private void validateContentType(String contentType) {
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new UnsupportedFileTypeException("Unsupported content type: " + contentType);
        }
    }

    private void validateMagicBytes(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            byte[] header = is.readNBytes(4);
            if (header.length < 4) {
                throw new InvalidResumeException("File is too small to be a valid document");
            }

            boolean isPdf = matches(header, PDF_MAGIC);
            boolean isZip = matches(header, ZIP_MAGIC);

            if (!isPdf && !isZip) {
                throw new UnsupportedFileTypeException(
                        "File content does not match declared type (invalid magic bytes)");
            }
        } catch (IOException e) {
            throw new InvalidResumeException("Could not read file for validation");
        }
    }

    private boolean matches(byte[] data, byte[] signature) {
        for (int i = 0; i < signature.length; i++) {
            if (data[i] != signature[i]) return false;
        }
        return true;
    }
}
