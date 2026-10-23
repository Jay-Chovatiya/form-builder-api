package com.dynamicform.api;

import com.dynamicform.api.common.validation.FormFieldValidator;
import com.dynamicform.api.dto.FileTypeConfigDto;
import com.dynamicform.api.dto.FormFieldDto;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class FormFieldValidatorTest {

    private FormFieldValidator validator;
    private ConstraintValidatorContext context;
    private ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder;
    private ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext nodeBuilder;

    @BeforeEach
    void setUp() {
        validator = new FormFieldValidator();
        context = Mockito.mock(ConstraintValidatorContext.class);
        violationBuilder = Mockito.mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
        nodeBuilder = Mockito.mock(ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext.class);

        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);
        when(violationBuilder.addPropertyNode(anyString())).thenReturn(nodeBuilder);
    }

    @Test
    @DisplayName("Form creation: File field with valid allowedTypes passes validation")
    void testValidFileFieldPasses() {
        FormFieldDto field = new FormFieldDto();
        field.setName("resume");
        field.setType("File");
        field.setLabel("Resume Document");
        field.setMaxSizeInBytes(5 * 1024 * 1024); // 5 MB
        field.setAllowedTypes(List.of(
                new FileTypeConfigDto(".pdf", "application/pdf"),
                new FileTypeConfigDto(".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
        ));

        assertTrue(validator.isValid(field, context));
    }

    @Test
    @DisplayName("Form creation: File field with dangerous extension (.exe) is rejected")
    void testDangerousExtensionRejected() {
        FormFieldDto field = new FormFieldDto();
        field.setName("fileUpload");
        field.setType("File");
        field.setLabel("Attachment");
        field.setAllowedTypes(List.of(
                new FileTypeConfigDto(".exe", "application/x-msdownload")
        ));

        assertFalse(validator.isValid(field, context));
    }

    @Test
    @DisplayName("Form creation: File field with dangerous script extension (.php) is rejected")
    void testDangerousScriptExtensionRejected() {
        FormFieldDto field = new FormFieldDto();
        field.setName("fileUpload");
        field.setType("File");
        field.setLabel("Attachment");
        field.setAllowedTypes(List.of(
                new FileTypeConfigDto(".php", "application/x-httpd-php")
        ));

        assertFalse(validator.isValid(field, context));
    }

    @Test
    @DisplayName("Form creation: File field with dangerous MIME type is rejected")
    void testDangerousMimeTypeRejected() {
        FormFieldDto field = new FormFieldDto();
        field.setName("fileUpload");
        field.setType("File");
        field.setLabel("Attachment");
        field.setAllowedTypes(List.of(
                new FileTypeConfigDto(".txt", "text/javascript")
        ));

        assertFalse(validator.isValid(field, context));
    }

    @Test
    @DisplayName("Form creation: File field exceeding server limit of 20MB is rejected")
    void testFileSizeExceedingServerLimitRejected() {
        FormFieldDto field = new FormFieldDto();
        field.setName("fileUpload");
        field.setType("File");
        field.setLabel("Attachment");
        field.setMaxSizeInBytes(25L * 1024L * 1024L); // 25 MB > 20 MB server limit
        field.setAllowedTypes(List.of(
                new FileTypeConfigDto(".pdf", "application/pdf")
        ));

        assertFalse(validator.isValid(field, context));
    }

    @Test
    @DisplayName("Form creation: File field without allowedTypes is rejected")
    void testFileFieldWithoutAllowedTypesRejected() {
        FormFieldDto field = new FormFieldDto();
        field.setName("fileUpload");
        field.setType("File");
        field.setLabel("Attachment");
        field.setAllowedTypes(new ArrayList<>());

        assertFalse(validator.isValid(field, context));
    }
}
