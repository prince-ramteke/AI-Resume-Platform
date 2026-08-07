package com.princeramteke.resumeai.jobdescription.validation;

import com.princeramteke.resumeai.resume.exception.InvalidResumeException;
import com.princeramteke.resumeai.resume.exception.ResumeTooLargeException;
import com.princeramteke.resumeai.resume.exception.UnsupportedFileTypeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JdFileValidatorTest {

    private JdFileValidator validator;

    private static final byte[] PDF_CONTENT = {0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34};
    private static final byte[] DOCX_CONTENT = {0x50, 0x4B, 0x03, 0x04, 0x14, 0x00, 0x06, 0x00};
    private static final byte[] TXT_CONTENT = "Senior Java Engineer needed".getBytes();

    @BeforeEach
    void setUp() {
        validator = new JdFileValidator();
    }

    @Test
    void validate_validPdf_passes() {
        var file = new MockMultipartFile("file", "jd.pdf", "application/pdf", PDF_CONTENT);
        assertThatCode(() -> validator.validate(file)).doesNotThrowAnyException();
    }

    @Test
    void validate_validDocx_passes() {
        var file = new MockMultipartFile("file", "jd.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                DOCX_CONTENT);
        assertThatCode(() -> validator.validate(file)).doesNotThrowAnyException();
    }

    @Test
    void validate_validTxt_passes() {
        var file = new MockMultipartFile("file", "jd.txt", "text/plain", TXT_CONTENT);
        assertThatCode(() -> validator.validate(file)).doesNotThrowAnyException();
    }

    @Test
    void validate_txtSkipsMagicBytes() {
        var file = new MockMultipartFile("file", "jd.txt", "text/plain",
                new byte[]{0x00, 0x01, 0x02});
        assertThatCode(() -> validator.validate(file)).doesNotThrowAnyException();
    }

    @Test
    void validate_nullFile_throwsInvalidResume() {
        assertThatThrownBy(() -> validator.validate(null))
                .isInstanceOf(InvalidResumeException.class);
    }

    @Test
    void validate_emptyFile_throwsInvalidResume() {
        var file = new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);
        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(InvalidResumeException.class);
    }

    @Test
    void validate_tooLarge_throwsResumeTooLarge() {
        byte[] largeContent = new byte[11 * 1024 * 1024];
        System.arraycopy(PDF_CONTENT, 0, largeContent, 0, PDF_CONTENT.length);
        var file = new MockMultipartFile("file", "big.pdf", "application/pdf", largeContent);
        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(ResumeTooLargeException.class);
    }

    @Test
    void validate_wrongExtension_throwsUnsupportedFileType() {
        var file = new MockMultipartFile("file", "jd.exe", "application/pdf", PDF_CONTENT);
        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(UnsupportedFileTypeException.class);
    }

    @Test
    void validate_wrongContentType_throwsUnsupportedFileType() {
        var file = new MockMultipartFile("file", "jd.pdf", "image/png", PDF_CONTENT);
        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(UnsupportedFileTypeException.class);
    }

    @Test
    void validate_wrongMagicBytes_throwsUnsupportedFileType() {
        byte[] fakeContent = {0x00, 0x01, 0x02, 0x03, 0x04, 0x05};
        var file = new MockMultipartFile("file", "jd.pdf", "application/pdf", fakeContent);
        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(UnsupportedFileTypeException.class);
    }
}
