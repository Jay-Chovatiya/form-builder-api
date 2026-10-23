package com.dynamicform.api;

import com.dynamicform.api.common.exception.BadRequestException;
import com.dynamicform.api.entity.FileTypeConfigEntity;
import com.dynamicform.api.entity.FormFieldEntity;
import com.dynamicform.api.service.filevalidation.FileValidationPipeline;
import com.dynamicform.api.service.filevalidation.FileValidationPipelineImpl;
import com.dynamicform.api.service.filevalidation.rule.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FileValidationPipelineTest {

    private FileValidationPipeline pipeline;
    private FormFieldEntity field;

    @BeforeEach
    void setUp() {
        pipeline = new FileValidationPipelineImpl(List.of(
                new FileMetadataValidationRule(),
                new FileExtensionValidationRule(),
                new FileMimeTypeValidationRule(),
                new FileSignatureValidationRule(),
                new FileContentSanitizationRule()
        ));

        field = new FormFieldEntity();
        field.setLabel("Resume Document");
        field.setMaxSizeInBytes(1024 * 1024); // 1 MB

        FileTypeConfigEntity pdfConfig = new FileTypeConfigEntity();
        pdfConfig.setExtension(".pdf");
        pdfConfig.setMimeType("application/pdf");

        FileTypeConfigEntity csvConfig = new FileTypeConfigEntity();
        csvConfig.setExtension(".csv");
        csvConfig.setMimeType("text/csv");

        FileTypeConfigEntity docxConfig = new FileTypeConfigEntity();
        docxConfig.setExtension(".docx");
        docxConfig.setMimeType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");

        field.setAllowedTypes(List.of(pdfConfig, csvConfig, docxConfig));
    }

    @Test
    @DisplayName("Layer 1 (Metadata): Empty file is rejected")
    void testLayer1_EmptyFileRejected() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "test.pdf", "application/pdf", new byte[0]);
        BadRequestException ex = assertThrows(BadRequestException.class, () -> pipeline.validate(emptyFile, field));
        assertTrue(ex.getMessage().contains("No file was uploaded or file is empty"));
    }

    @Test
    @DisplayName("Layer 1 (Metadata): Path traversal in filename is rejected")
    void testLayer1_PathTraversalRejected() {
        MockMultipartFile traversalFile = new MockMultipartFile("file", "../test.pdf", "application/pdf", new byte[]{0x25, 0x50});
        BadRequestException ex = assertThrows(BadRequestException.class, () -> pipeline.validate(traversalFile, field));
        assertTrue(ex.getMessage().contains("invalid path traversal characters"));
    }

    @Test
    @DisplayName("Layer 1 (Metadata): File exceeding field maxSizeInBytes is rejected")
    void testLayer1_MaxSizeExceededRejected() {
        field.setMaxSizeInBytes(100);
        MockMultipartFile bigFile = new MockMultipartFile("file", "test.pdf", "application/pdf", new byte[200]);
        BadRequestException ex = assertThrows(BadRequestException.class, () -> pipeline.validate(bigFile, field));
        assertTrue(ex.getMessage().contains("exceeds the maximum allowed size"));
    }

    @Test
    @DisplayName("Layer 2 (Extension): Double extension attack is rejected")
    void testLayer2_DoubleExtensionAttackRejected() {
        MockMultipartFile doubleExtFile = new MockMultipartFile("file", "report.php.pdf", "application/pdf", new byte[]{0x25, 0x50, 0x44, 0x46});
        BadRequestException ex = assertThrows(BadRequestException.class, () -> pipeline.validate(doubleExtFile, field));
        assertTrue(ex.getMessage().contains("suspicious double extension"));
    }

    @Test
    @DisplayName("Layer 2 (Extension): Unlisted extension is rejected")
    void testLayer2_UnlistedExtensionRejected() {
        MockMultipartFile badExt = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8});
        BadRequestException ex = assertThrows(BadRequestException.class, () -> pipeline.validate(badExt, field));
        assertTrue(ex.getMessage().contains("unsupported extension"));
    }

    @Test
    @DisplayName("Layer 3 (MIME Type): Mismatched MIME type is rejected")
    void testLayer3_MismatchedMimeTypeRejected() {
        MockMultipartFile badMime = new MockMultipartFile("file", "doc.pdf", "image/png", new byte[]{0x25, 0x50, 0x44, 0x46});
        BadRequestException ex = assertThrows(BadRequestException.class, () -> pipeline.validate(badMime, field));
        assertTrue(ex.getMessage().contains("does not match the allowed MIME type"));
    }

    @Test
    @DisplayName("Layer 4 (Signatures): Disguised Windows executable (MZ header) is rejected")
    void testLayer4_ExecutableDisguisedAsPdfRejected() {
        byte[] mzExe = new byte[]{(byte) 0x4D, (byte) 0x5A, 0x00, 0x00, 0x00};
        MockMultipartFile fakePdf = new MockMultipartFile("file", "virus.pdf", "application/pdf", mzExe);
        BadRequestException ex = assertThrows(BadRequestException.class, () -> pipeline.validate(fakePdf, field));
        assertTrue(ex.getMessage().contains("executable binary content (MZ header)"));
    }

    @Test
    @DisplayName("Layer 4 (Signatures): PDF without %PDF magic bytes is rejected")
    void testLayer4_CorruptedPdfSignatureRejected() {
        byte[] invalidSig = new byte[]{0x01, 0x02, 0x03, 0x04};
        MockMultipartFile fakePdf = new MockMultipartFile("file", "invalid.pdf", "application/pdf", invalidSig);
        BadRequestException ex = assertThrows(BadRequestException.class, () -> pipeline.validate(fakePdf, field));
        assertTrue(ex.getMessage().contains("does not match the expected binary signature"));
    }

    @Test
    @DisplayName("Layer 5 (Content Sanitization): Active XSS in CSV is rejected")
    void testLayer5_ActiveXssInCsvRejected() {
        String xssPayload = "id,name\n1,<script>alert(1)</script>";
        MockMultipartFile xssFile = new MockMultipartFile("file", "data.csv", "text/csv", xssPayload.getBytes(StandardCharsets.UTF_8));
        BadRequestException ex = assertThrows(BadRequestException.class, () -> pipeline.validate(xssFile, field));
        assertTrue(ex.getMessage().contains("active script or HTML tags"));
    }

    @Test
    @DisplayName("Layer 5 (Content Sanitization): DOCX containing VBA macros is rejected")
    void testLayer5_DocxWithVbaProjectRejected() {
        byte[] docxHeader = new byte[]{0x50, 0x4B, 0x03, 0x04};
        byte[] marker = "word/vbaProject.bin".getBytes(StandardCharsets.US_ASCII);
        byte[] payload = new byte[docxHeader.length + marker.length + 8];
        System.arraycopy(docxHeader, 0, payload, 0, docxHeader.length);
        System.arraycopy(marker, 0, payload, docxHeader.length, marker.length);

        MockMultipartFile macroFile = new MockMultipartFile(
                "file",
                "macro.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                payload
        );
        BadRequestException ex = assertThrows(BadRequestException.class, () -> pipeline.validate(macroFile, field));
        assertTrue(ex.getMessage().contains("embedded VBA macros"));
    }

    @Test
    @DisplayName("Layer 5 (Content Sanitization): PDF containing embedded JavaScript is rejected")
    void testLayer5_PdfWithEmbeddedJavaScriptRejected() {
        String jsPdf = "%PDF-1.5\n<< /Type /Action /S /JavaScript /JS (app.alert('PWNED');) >>";
        MockMultipartFile pdfFile = new MockMultipartFile("file", "action.pdf", "application/pdf", jsPdf.getBytes(StandardCharsets.US_ASCII));
        BadRequestException ex = assertThrows(BadRequestException.class, () -> pipeline.validate(pdfFile, field));
        assertTrue(ex.getMessage().contains("embedded PDF JavaScript actions"));
    }

    @Test
    @DisplayName("Valid clean PDF passes all 5 security pipeline layers")
    void testCleanPdfPasses() {
        String cleanPdf = "%PDF-1.5\n1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\ntrailer\n<< /Root 1 0 R >>";
        MockMultipartFile validPdf = new MockMultipartFile("file", "clean.pdf", "application/pdf", cleanPdf.getBytes(StandardCharsets.US_ASCII));
        assertDoesNotThrow(() -> pipeline.validate(validPdf, field));
    }
}
