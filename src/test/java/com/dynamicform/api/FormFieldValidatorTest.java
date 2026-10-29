package com.dynamicform.api;

import com.dynamicform.api.common.validation.FormFieldValidator;
import com.dynamicform.api.dto.FieldOptionDto;
import com.dynamicform.api.dto.FileTypeConfigDto;
import com.dynamicform.api.dto.FieldValidationDto;
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

    @Test
    @DisplayName("Dropdown field without options is rejected")
    void testDropdownWithoutOptionsRejected() {
        FormFieldDto field = new FormFieldDto();
        field.setName("country");
        field.setType("Dropdown");
        field.setLabel("Country");
        field.setOptions(new ArrayList<>());

        assertFalse(validator.isValid(field, context));
    }

    @Test
    @DisplayName("RadioButton field without options is rejected")
    void testRadioButtonWithoutOptionsRejected() {
        FormFieldDto field = new FormFieldDto();
        field.setName("gender");
        field.setType("RadioButton");
        field.setLabel("Gender");
        field.setOptions(null);

        assertFalse(validator.isValid(field, context));
    }

    @Test
    @DisplayName("Dropdown field with duplicate option values is rejected")
    void testDropdownDuplicateOptionsRejected() {
        FormFieldDto field = new FormFieldDto();
        field.setName("country");
        field.setType("Dropdown");
        field.setLabel("Country");
        field.setOptions(List.of(
                new FieldOptionDto(null, "India", "IN", 1),
                new FieldOptionDto(null, "India Republic", "in", 2)
        ));

        assertFalse(validator.isValid(field, context));
    }

    @Test
    @DisplayName("Dropdown field with valid options passes")
    void testDropdownWithValidOptionsPasses() {
        FormFieldDto field = new FormFieldDto();
        field.setName("country");
        field.setType("Dropdown");
        field.setLabel("Country");
        field.setOptions(List.of(
                new FieldOptionDto(null, "India", "IN", 1),
                new FieldOptionDto(null, "United States", "US", 2)
        ));

        assertTrue(validator.isValid(field, context));
    }

    @Test
    @DisplayName("Text field with regex pattern matching default value passes")
    void testRegexPatternMatchingDefaultValuePasses() {
        FormFieldDto field = new FormFieldDto();
        field.setName("zipCode");
        field.setType("Text");
        field.setLabel("Zip Code");
        field.setDefaultValue("12345");
        field.setValidations(List.of(
                new FieldValidationDto("pattern", "^\\d{5}$")
        ));

        assertTrue(validator.isValid(field, context));
    }

    @Test
    @DisplayName("Text field with regex pattern not matching default value is rejected")
    void testRegexPatternNotMatchingDefaultValueRejected() {
        FormFieldDto field = new FormFieldDto();
        field.setName("zipCode");
        field.setType("Text");
        field.setLabel("Zip Code");
        field.setDefaultValue("ABCDE");
        field.setValidations(List.of(
                new FieldValidationDto("pattern", "^\\d{5}$")
        ));

        assertFalse(validator.isValid(field, context));
    }

    @Test
    @DisplayName("Field with malformed regex pattern syntax is rejected")
    void testInvalidRegexPatternSyntaxRejected() {
        FormFieldDto field = new FormFieldDto();
        field.setName("zipCode");
        field.setType("Text");
        field.setLabel("Zip Code");
        field.setValidations(List.of(
                new FieldValidationDto("pattern", "[0-9++")
        ));

        assertFalse(validator.isValid(field, context));
    }

    @Test
    @DisplayName("Validation rule 'pattern' on non-text field is rejected")
    void testPatternOnNonTextFieldRejected() {
        FormFieldDto field = new FormFieldDto();
        field.setName("count");
        field.setType("Number");
        field.setLabel("Count");
        field.setValidations(List.of(
                new FieldValidationDto("pattern", "^\\d+$")
        ));

        assertFalse(validator.isValid(field, context));
    }

    @Test
    @DisplayName("Validation rule 'minLength' on Number field is rejected")
    void testMinLengthOnNumberFieldRejected() {
        FormFieldDto field = new FormFieldDto();
        field.setName("age");
        field.setType("Number");
        field.setLabel("Age");
        field.setValidations(List.of(
                new FieldValidationDto("minLength", "2")
        ));

        assertFalse(validator.isValid(field, context));
    }

    @Test
    @DisplayName("Validation rule 'minValue' on Text field is rejected")
    void testMinValueOnTextFieldRejected() {
        FormFieldDto field = new FormFieldDto();
        field.setName("username");
        field.setType("Text");
        field.setLabel("Username");
        field.setValidations(List.of(
                new FieldValidationDto("minValue", "5")
        ));

        assertFalse(validator.isValid(field, context));
    }

    @Test
    @DisplayName("Date field default value format valid passes")
    void testDateDefaultValuePasses() {
        FormFieldDto field = new FormFieldDto();
        field.setName("dob");
        field.setType("Date");
        field.setLabel("Date of Birth");
        field.setDefaultValue("2026-10-29");

        assertTrue(validator.isValid(field, context));
    }

    @Test
    @DisplayName("Date field default value format invalid is rejected")
    void testDateDefaultValueInvalidRejected() {
        FormFieldDto field = new FormFieldDto();
        field.setName("dob");
        field.setType("Date");
        field.setLabel("Date of Birth");
        field.setDefaultValue("29-10-2026");

        assertFalse(validator.isValid(field, context));
    }
}
