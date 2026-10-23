package com.dynamicform.api.service.filevalidation.rule;

import com.dynamicform.api.common.exception.BadRequestException;
import com.dynamicform.api.entity.FormFieldEntity;
import com.dynamicform.api.service.filevalidation.FileValidationRule;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Layer 5: File Content Sanitization & Active Payload Inspection:
 * - Inspects file content for embedded script payloads, macro patterns, and active XSS vectors
 * - Web text/SVG: blocks <script>, javascript:, onload/onerror handlers, <iframe>, <object>, <embed>
 * - Office documents (.docx, .xlsx, .pptx): blocks embedded VBA macros (vbaProject.bin)
 * - PDF documents: blocks embedded PDF JavaScript actions (/JavaScript, /JS)
 */
@Component
@Order(5)
public class FileContentSanitizationRule implements FileValidationRule {

    private static final Set<String> WEB_TEXT_EXTENSIONS = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    static {
        WEB_TEXT_EXTENSIONS.addAll(Arrays.asList(".svg", ".xml", ".html", ".htm", ".txt", ".csv", ".json"));
    }

    private static final Pattern XSS_PATTERN_REGEX = Pattern.compile(
            "<\\s*script[^>]*>|javascript\\s*:|vbscript\\s*:|on(load|error|click|mouseover|submit|focus|blur)\\s*=|" +
            "<\\s*iframe[^>]*>|<\\s*object[^>]*>|<\\s*embed[^>]*>",
            Pattern.CASE_INSENSITIVE);

    private static final byte[] OPEN_OFFICE_VBA_PROJECT_MARKER = "vbaProject.bin".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] PDF_JAVASCRIPT_MARKER = "/JavaScript".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] PDF_JS_MARKER = "/JS".getBytes(StandardCharsets.US_ASCII);

    private static final int MAX_BUFFER_INSPECTION_BYTES = 32 * 1024;

    @Override
    public void validate(MultipartFile file, FormFieldEntity field) {
        String fileName = Paths.get(file.getOriginalFilename()).getFileName().toString().trim();
        int dotIndex = fileName.lastIndexOf('.');
        String extension = dotIndex >= 0 ? fileName.substring(dotIndex).toLowerCase() : "";

        int bytesToRead = (int) Math.min(file.getSize(), MAX_BUFFER_INSPECTION_BYTES);
        byte[] buffer = new byte[bytesToRead];
        int bytesRead;

        try (InputStream in = file.getInputStream()) {
            bytesRead = in.read(buffer, 0, bytesToRead);
        } catch (IOException e) {
            throw new BadRequestException("Failed to read file stream for content inspection.");
        }

        if (bytesRead <= 0) {
            return;
        }

        if (WEB_TEXT_EXTENSIONS.contains(extension)) {
            String textSnippet = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
            if (XSS_PATTERN_REGEX.matcher(textSnippet).find()) {
                throw new BadRequestException(String.format(
                        "File '%s' is rejected because it contains active script or HTML tags.", fileName));
            }
        }

        if (extension.equals(".docx") || extension.equals(".xlsx") || extension.equals(".pptx")) {
            if (containsByteSequence(buffer, bytesRead, OPEN_OFFICE_VBA_PROJECT_MARKER)) {
                throw new BadRequestException(String.format(
                        "File '%s' is rejected because it contains embedded VBA macros.", fileName));
            }
        }

        if (extension.equals(".pdf")) {
            if (containsByteSequence(buffer, bytesRead, PDF_JAVASCRIPT_MARKER) ||
                containsByteSequence(buffer, bytesRead, PDF_JS_MARKER)) {
                throw new BadRequestException(String.format(
                        "File '%s' is rejected because it contains embedded PDF JavaScript actions.", fileName));
            }
        }
    }

    private static boolean containsByteSequence(byte[] buffer, int length, byte[] sequence) {
        if (length < sequence.length) return false;

        for (int i = 0; i <= length - sequence.length; i++) {
            boolean match = true;
            for (int j = 0; j < sequence.length; j++) {
                if (buffer[i + j] != sequence[j]) {
                    match = false;
                    break;
                }
            }
            if (match) return true;
        }
        return false;
    }
}
