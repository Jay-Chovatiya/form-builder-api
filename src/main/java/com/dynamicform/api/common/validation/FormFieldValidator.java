package com.dynamicform.api.common.validation;

import com.dynamicform.api.common.constant.FileSecurityConstants;
import com.dynamicform.api.dto.FieldOptionDto;
import com.dynamicform.api.dto.FileTypeConfigDto;
import com.dynamicform.api.dto.FieldValidationDto;
import com.dynamicform.api.dto.FormFieldDto;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Component
public class FormFieldValidator implements ConstraintValidator<FormFieldValid, FormFieldDto> {

    private static final String ALLOWED_TYPES = "allowedTypes";
    private static final String MAX_SIZE = "maxSizeInBytes";
    private static final String VALIDATIONS = "validations";
    private static final String DEFAULT_VALUE = "defaultValue";
    private static final String OPTIONS = "options";

    private static final Pattern MIME_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9!#$&^_\\.\\+-]+/[a-zA-Z0-9!#$&^_\\.\\+-]+$");

    @Value("${spring.servlet.multipart.max-file-size:20MB}")
    private DataSize maxFileSize = DataSize.ofMegabytes(20);

    @Override
    public boolean isValid(FormFieldDto dto, ConstraintValidatorContext context) {
        if (dto == null) {
            return true;
        }

        boolean valid = true;
        context.disableDefaultConstraintViolation();

        String fieldName = resolveFieldDisplayName(dto);

        // File type & file size configuration validation
        if ("File".equalsIgnoreCase(dto.getType())) {
            return validateFileConfiguration(dto, fieldName, context);
        }

        // Dropdown / RadioButton options requirement validation
        if (isChoiceField(dto.getType())) {
            valid &= validateOptionConfiguration(dto, fieldName, context);
        }

        // Validation rules consistency checks (minLength vs maxLength, minValue vs maxValue, regex syntax)
        if (dto.getValidations() != null && !dto.getValidations().isEmpty()) {
            valid &= validateRulesConsistency(dto, fieldName, context);
        }

        // Default value validations
        if (dto.getDefaultValue() != null && !dto.getDefaultValue().isBlank()) {
            valid &= validateDefaultValue(dto, fieldName, context);
        }

        return valid;
    }

    private String resolveFieldDisplayName(FormFieldDto dto) {
        if (dto.getLabel() != null && !dto.getLabel().isBlank()) {
            return dto.getLabel().trim();
        }
        return dto.getName() != null && !dto.getName().isBlank() ? dto.getName().trim() : "Field";
    }

    private void addViolation(ConstraintValidatorContext ctx, String propertyPath, String message) {
        ctx.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(propertyPath)
                .addConstraintViolation();
    }

    private boolean validateFileConfiguration(FormFieldDto dto, String fieldName, ConstraintValidatorContext ctx) {
        boolean valid = true;

        if (dto.getAllowedTypes() == null || dto.getAllowedTypes().isEmpty()) {
            addViolation(ctx, ALLOWED_TYPES,
                    String.format("Please select at least one allowed file type for field '%s'.", fieldName));
            valid = false;
        } else {
            Set<String> seenExtensions = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            for (FileTypeConfigDto ft : dto.getAllowedTypes()) {
                valid &= validateFileExtension(ft, fieldName, seenExtensions, ctx);
                valid &= validateFileMimeType(ft, fieldName, ctx);
            }
        }

        valid &= validateMaxFileSize(dto, fieldName, ctx);
        return valid;
    }

    private boolean validateFileExtension(FileTypeConfigDto ft, String fieldName, Set<String> seenExtensions, ConstraintValidatorContext ctx) {
        if (ft.getExtension() == null || ft.getExtension().isBlank()) {
            addViolation(ctx, ALLOWED_TYPES,
                    String.format("File extension cannot be empty for field '%s'.", fieldName));
            return false;
        }

        String ext = ft.getExtension().trim();
        if (!ext.startsWith(".") || ext.length() < 2) {
            addViolation(ctx, ALLOWED_TYPES,
                    String.format("File extension '%s' for field '%s' must start with '.' followed by extension name.",
                            ext, fieldName));
            return false;
        }

        if (!seenExtensions.add(ext)) {
            addViolation(ctx, ALLOWED_TYPES,
                    String.format("Duplicate file extension '%s' specified for field '%s'.", ext, fieldName));
            return false;
        }

        if (FileSecurityConstants.DANGEROUS_EXTENSIONS.contains(ext)) {
            addViolation(ctx, ALLOWED_TYPES,
                    String.format("File extension '%s' for field '%s' is prohibited for security reasons.",
                            ext, fieldName));
            return false;
        }

        return true;
    }

    private boolean validateFileMimeType(FileTypeConfigDto ft, String fieldName, ConstraintValidatorContext ctx) {
        if (ft.getMimeType() == null || ft.getMimeType().isBlank()) {
            addViolation(ctx, ALLOWED_TYPES,
                    String.format("MIME type cannot be empty for file extension '%s' in field '%s'.",
                            ft.getExtension(), fieldName));
            return false;
        }

        String mime = ft.getMimeType().trim();
        if (!MIME_PATTERN.matcher(mime).matches()) {
            addViolation(ctx, ALLOWED_TYPES,
                    String.format("Invalid MIME type '%s' specified for field '%s'.", mime, fieldName));
            return false;
        }

        if (FileSecurityConstants.DANGEROUS_MIME_TYPES.contains(mime)) {
            addViolation(ctx, ALLOWED_TYPES,
                    String.format("MIME type '%s' for field '%s' is prohibited for security reasons.",
                            mime, fieldName));
            return false;
        }

        return true;
    }

    private boolean validateMaxFileSize(FormFieldDto dto, String fieldName, ConstraintValidatorContext ctx) {
        long maxServerBytes = maxFileSize != null ? maxFileSize.toBytes() : 20L * 1024L * 1024L;
        long maxServerMb = maxFileSize != null ? maxFileSize.toMegabytes() : 20L;

        if (dto.getMaxSizeInBytes() > maxServerBytes) {
            addViolation(ctx, MAX_SIZE,
                    String.format("Field '%s' max file size (%d bytes) cannot exceed server upload limit of %d MB (%d bytes).",
                            fieldName, dto.getMaxSizeInBytes(), maxServerMb, maxServerBytes));
            return false;
        }
        return true;
    }

    private boolean validateOptionConfiguration(FormFieldDto dto, String fieldName, ConstraintValidatorContext ctx) {
        if (dto.getOptions() == null || dto.getOptions().isEmpty()) {
            addViolation(ctx, OPTIONS,
                    String.format("Field '%s' of type '%s' requires at least one option.", fieldName, dto.getType()));
            return false;
        }

        boolean valid = true;
        Set<String> seenValues = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (FieldOptionDto opt : dto.getOptions()) {
            if (opt != null && opt.getValue() != null && !opt.getValue().isBlank()) {
                String val = opt.getValue().trim();
                if (!seenValues.add(val)) {
                    addViolation(ctx, OPTIONS,
                            String.format("Duplicate option value '%s' specified for field '%s'.", val, fieldName));
                    valid = false;
                }
            }
        }
        return valid;
    }

    private boolean validateRulesConsistency(FormFieldDto dto, String fieldName, ConstraintValidatorContext ctx) {
        boolean valid = true;
        Integer minLength = null;
        Integer maxLength = null;
        Double minValue = null;
        Double maxValue = null;

        for (FieldValidationDto v : dto.getValidations()) {
            if (v.getType() == null || v.getValue() == null) continue;
            String type = v.getType().trim().toLowerCase();
            String valStr = v.getValue().toString().trim();

            switch (type) {
                case "minlength":
                    if (!dto.getType().isBlank() && !isTextField(dto.getType())) {
                        addViolation(ctx, VALIDATIONS,
                                String.format("Validation rule 'minLength' is not applicable for field '%s' of type '%s'.",
                                        fieldName, dto.getType()));
                        valid = false;
                    } else {
                        minLength = tryParseInteger(valStr);
                    }
                    break;
                case "maxlength":
                    if (!dto.getType().isBlank() && !isTextField(dto.getType())) {
                        addViolation(ctx, VALIDATIONS,
                                String.format("Validation rule 'maxLength' is not applicable for field '%s' of type '%s'.",
                                        fieldName, dto.getType()));
                        valid = false;
                    } else {
                        maxLength = tryParseInteger(valStr);
                    }
                    break;
                case "minvalue":
                    if (!dto.getType().isBlank() && !isNumericField(dto.getType())) {
                        addViolation(ctx, VALIDATIONS,
                                String.format("Validation rule 'minValue' is not applicable for field '%s' of type '%s'.",
                                        fieldName, dto.getType()));
                        valid = false;
                    } else {
                        minValue = tryParseDouble(valStr);
                    }
                    break;
                case "maxvalue":
                    if (!dto.getType().isBlank() && !isNumericField(dto.getType())) {
                        addViolation(ctx, VALIDATIONS,
                                String.format("Validation rule 'maxValue' is not applicable for field '%s' of type '%s'.",
                                        fieldName, dto.getType()));
                        valid = false;
                    } else {
                        maxValue = tryParseDouble(valStr);
                    }
                    break;
                case "pattern":
                    if (!dto.getType().isBlank() && !isTextField(dto.getType())) {
                        addViolation(ctx, VALIDATIONS,
                                String.format("Validation rule 'pattern' is not applicable for field '%s' of type '%s'.",
                                        fieldName, dto.getType()));
                        valid = false;
                    } else if (!valStr.isEmpty()) {
                        try {
                            Pattern.compile(valStr);
                        } catch (PatternSyntaxException e) {
                            addViolation(ctx, VALIDATIONS,
                                    String.format("Pattern '%s' for field '%s' is not a valid regular expression.",
                                            valStr, fieldName));
                            valid = false;
                        }
                    }
                    break;
                default:
                    break;
            }
        }

        if (minLength != null && maxLength != null && minLength > maxLength) {
            addViolation(ctx, VALIDATIONS,
                    String.format("Minimum length (%d) cannot be greater than maximum length (%d) for field '%s'.",
                            minLength, maxLength, fieldName));
            valid = false;
        }

        if (minValue != null && maxValue != null && minValue > maxValue) {
            addViolation(ctx, VALIDATIONS,
                    String.format("Minimum value (%s) cannot be greater than maximum value (%s) for field '%s'.",
                            minValue, maxValue, fieldName));
            valid = false;
        }

        return valid;
    }

    private boolean validateDefaultValue(FormFieldDto dto, String fieldName, ConstraintValidatorContext ctx) {
        boolean valid = true;
        String trimmedDefault = dto.getDefaultValue().trim();

        // 1. Date / DateTime format validation
        if (isDateField(dto.getType())) {
            return validateDefaultDateFormat(dto.getType(), trimmedDefault, fieldName, ctx);
        }

        // 2. Dropdown / RadioButton options membership validation
        if (isChoiceField(dto.getType())) {
            return validateDefaultOptionMatch(dto, trimmedDefault, fieldName, ctx);
        }

        // 3. Min/Max numeric, length limit, and regex pattern validations
        if (isTextField(dto.getType()) || isNumericField(dto.getType())) {
            if (dto.getValidations() != null && !dto.getValidations().isEmpty()) {
                valid &= validateDefaultAgainstConfiguredLimits(dto.getValidations(), trimmedDefault, fieldName, ctx);
            }
        }

        return valid;
    }


    private boolean validateDefaultDateFormat(String fieldType, String trimmedDefault, String fieldName, ConstraintValidatorContext ctx) {
        if ("Date".equalsIgnoreCase(fieldType)) {
            try {
                LocalDate.parse(trimmedDefault);
            } catch (DateTimeParseException e) {
                addViolation(ctx, DEFAULT_VALUE,
                        String.format("Default value '%s' for field '%s' must be a valid date value.",
                                trimmedDefault, fieldName));
                return false;
            }
        } else if ("DateTime".equalsIgnoreCase(fieldType)) {
            try {
                LocalDateTime.parse(trimmedDefault);
            } catch (DateTimeParseException e) {
                addViolation(ctx, DEFAULT_VALUE,
                        String.format("Default value '%s' for field '%s' must be a valid date and time value.",
                                trimmedDefault, fieldName));
                return false;
            }
        }
        return true;
    }

    private boolean validateDefaultOptionMatch(FormFieldDto dto, String trimmedDefault, String fieldName, ConstraintValidatorContext ctx) {
        if (dto.getOptions() == null || dto.getOptions().isEmpty()) {
            return true;
        }

        boolean match = dto.getOptions().stream()
                .map(FieldOptionDto::getValue)
                .filter(val -> val != null && !val.isBlank())
                .anyMatch(val -> val.trim().equalsIgnoreCase(trimmedDefault));

        if (!match) {
            addViolation(ctx, DEFAULT_VALUE,
                    String.format("Default value '%s' must be one of the defined options for field '%s'.",
                            trimmedDefault, fieldName));
            return false;
        }

        return true;
    }

    private boolean validateDefaultAgainstConfiguredLimits(Iterable<FieldValidationDto> validations, String trimmedDefault, String fieldName, ConstraintValidatorContext ctx) {
        boolean valid = true;

        for (FieldValidationDto v : validations) {
            if (v.getType() == null || v.getValue() == null) {
                continue;
            }
            String type = v.getType().trim().toLowerCase();

            switch (type) {
                case "minvalue":
                    valid &= checkMinValueLimit(v, trimmedDefault, fieldName, ctx);
                    break;
                case "maxvalue":
                    valid &= checkMaxValueLimit(v, trimmedDefault, fieldName, ctx);
                    break;
                case "minlength":
                    valid &= checkMinLengthLimit(v, trimmedDefault, fieldName, ctx);
                    break;
                case "maxlength":
                    valid &= checkMaxLengthLimit(v, trimmedDefault, fieldName, ctx);
                    break;
                case "pattern":
                    valid &= checkRegexPattern(v, trimmedDefault, fieldName, ctx);
                    break;
                default:
                    break;
            }
        }

        return valid;
    }

    private boolean checkMinValueLimit(FieldValidationDto v, String trimmedDefault, String fieldName, ConstraintValidatorContext ctx) {
        Double min = tryParseDouble(v.getValue().toString());
        Double val = tryParseDouble(trimmedDefault);

        if (min != null && val != null && val < min) {
            addViolation(ctx, DEFAULT_VALUE,
                    String.format("Default value '%s' for field '%s' cannot be less than minimum value %s.",
                            trimmedDefault, fieldName, min));
            return false;
        }
        return true;
    }

    private boolean checkMaxValueLimit(FieldValidationDto v, String trimmedDefault, String fieldName, ConstraintValidatorContext ctx) {
        Double max = tryParseDouble(v.getValue().toString());
        Double val = tryParseDouble(trimmedDefault);

        if (max != null && val != null && val > max) {
            addViolation(ctx, DEFAULT_VALUE,
                    String.format("Default value '%s' for field '%s' cannot be greater than maximum value %s.",
                            trimmedDefault, fieldName, max));
            return false;
        }
        return true;
    }

    private boolean checkMinLengthLimit(FieldValidationDto v, String trimmedDefault, String fieldName, ConstraintValidatorContext ctx) {
        Integer minLen = tryParseInteger(v.getValue().toString());

        if (minLen != null && trimmedDefault.length() < minLen) {
            addViolation(ctx, DEFAULT_VALUE,
                    String.format("Default value for field '%s' must have a minimum length of %d characters.",
                            fieldName, minLen));
            return false;
        }
        return true;
    }

    private boolean checkMaxLengthLimit(FieldValidationDto v, String trimmedDefault, String fieldName, ConstraintValidatorContext ctx) {
        Integer maxLen = tryParseInteger(v.getValue().toString());

        if (maxLen != null && trimmedDefault.length() > maxLen) {
            addViolation(ctx, DEFAULT_VALUE,
                    String.format("Default value for field '%s' cannot exceed maximum length of %d characters.",
                            fieldName, maxLen));
            return false;
        }
        return true;
    }

    private boolean checkRegexPattern(FieldValidationDto v, String trimmedDefault, String fieldName, ConstraintValidatorContext ctx) {
        if (v.getValue() == null) {
            return true;
        }
        String patternStr = v.getValue().toString().trim();
        if (patternStr.isEmpty()) {
            return true;
        }

        try {
            Pattern pattern = Pattern.compile(patternStr);
            if (!pattern.matcher(trimmedDefault).matches()) {
                addViolation(ctx, DEFAULT_VALUE,
                        String.format("Default value '%s' for field '%s' does not match the configured pattern '%s'.",
                                trimmedDefault, fieldName, patternStr));
                return false;
            }
        } catch (PatternSyntaxException e) {
            addViolation(ctx, VALIDATIONS,
                    String.format("Pattern '%s' for field '%s' is not a valid regular expression.",
                            patternStr, fieldName));
            return false;
        }

        return true;
    }

    private boolean isTextField(String type) {
        return "Textbox".equalsIgnoreCase(type) || "Text".equalsIgnoreCase(type) || "Textarea".equalsIgnoreCase(type);
    }

    private boolean isNumericField(String type) {
        return "Number".equalsIgnoreCase(type);
    }

    private boolean isChoiceField(String type) {
        return "Dropdown".equalsIgnoreCase(type) || "RadioButton".equalsIgnoreCase(type);
    }

    private boolean isDateField(String type) {
        return "Date".equalsIgnoreCase(type) || "DateTime".equalsIgnoreCase(type);
    }

    private Double tryParseDouble(String val) {
        try {
            return Double.valueOf(val.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private Integer tryParseInteger(String val) {
        try {
            return Integer.valueOf(val.trim());
        } catch (Exception e) {
            return null;
        }
    }
}
