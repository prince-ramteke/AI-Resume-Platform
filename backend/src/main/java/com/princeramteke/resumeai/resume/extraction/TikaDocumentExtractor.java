package com.princeramteke.resumeai.resume.extraction;

import com.princeramteke.resumeai.resume.exception.ExtractionException;
import com.princeramteke.resumeai.resume.exception.InvalidResumeException;
import org.apache.tika.exception.EncryptedDocumentException;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

@Component
public class TikaDocumentExtractor implements DocumentExtractor {

    private static final Logger log = LoggerFactory.getLogger(TikaDocumentExtractor.class);

    @Override
    public ExtractionResult extract(MultipartFile file) {
        log.info("Extraction started: filename={}", file.getOriginalFilename());
        try (InputStream is = file.getInputStream()) {
            var handler = new BodyContentHandler(-1);
            var metadata = new Metadata();
            metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, file.getOriginalFilename());
            new AutoDetectParser().parse(is, handler, metadata);

            String text = normalizeText(handler.toString());
            if (text.isBlank()) {
                throw new InvalidResumeException("No readable text found in document");
            }

            Integer pageCount = parseInteger(metadata.get("xmpTPg:NPages"));
            if (pageCount == null) {
                pageCount = parseInteger(metadata.get("meta:page-count"));
            }
            String language = metadata.get(TikaCoreProperties.LANGUAGE);

            log.info("Extraction completed: filename={}, chars={}, pages={}",
                    file.getOriginalFilename(), text.length(), pageCount);
            return new ExtractionResult(text, pageCount, language);

        } catch (EncryptedDocumentException e) {
            throw new InvalidResumeException("Password-protected documents are not supported");
        } catch (TikaException | SAXException e) {
            throw new ExtractionException("Failed to extract text from document", e);
        } catch (IOException e) {
            throw new ExtractionException("Failed to read document for extraction", e);
        }
    }

    private String normalizeText(String text) {
        if (text == null) return "";
        return text.replaceAll("\\r\\n", "\n")
                .replaceAll("\\r", "\n")
                .replaceAll("[ \\t]+\\n", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .strip();
    }

    private Integer parseInteger(String value) {
        if (value == null) return null;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
