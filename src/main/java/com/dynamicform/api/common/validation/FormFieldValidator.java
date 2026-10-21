package com.dynamicform.api.common.validation;

import com.dynamicform.api.dto.FormFieldDto;
import com.dynamicform.api.dto.FileTypeConfigDto;
import com.dynamicform.api.dto.FieldValidationDto;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Set;
import java.util.TreeSet;

public class FormFieldValidator implements ConstraintValidator<FormFieldValid, FormFieldDto> {

    @Override
    public boolean isValid(FormFieldDto dto, ConstraintValidatorContext context) {
        boolean valid = true;
        context.disableDefaultConstraintViolation();

        String fieldDisplayName = (dto.getLabel() != null && !dto.getLabel().isBlank())
                ? dto.getLabel().trim()
                : (dto.getName() != null ? dto.getName().trim() : "Field");

        // File type validation
        if ("File".equalsIgnoreCase(dto.getType())) {
            if (dto.getAllowedTypes() == null || dto.getAllowedTypes().isEmpty()) {
                valid = false;
                context.buildConstraintViolationWithTemplate(
                                String.format("Please select at least one allowed file type for field '%s'.", fieldDisplayName))
                        .addPropertyNode("allowedTypes")
                        .addConstraintViolation();
            } else {
                Set<String> seenExtensions = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
                for (FileTypeConfigDto ft : dto.getAllowedTypes()) {
                    if (ft.getExtension() != null && !ft.getExtension().isBlank()) {
                        String ext = ft.getExtension().trim();
                        if (!seenExtensions.add(ext)) {
                            valid = false;
                            context.buildConstraintViolationWithTemplate(
                                            String.format(
                                                    "Duplicate file extension '%s' specified for field '%s'.",
                                                    ext, fieldDisplayName))
                                    .addPropertyNode("allowedTypes")
                                    .addConstraintViolation();
                        }
                    }
                }
            }
        }

        // Validation rules consistency checks (minLength vs maxLength, minValue vs maxValue)
        if (dto.getValidations() != null && !dto.getValidations().isEmpty()) {
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
                        try {
                            minLength = Integer.parseInt(valStr);
                        } catch (NumberFormatException ignored) {}
                        break;
                    case "maxlength":
                        try {
                            maxLength = Integer.parseInt(valStr);
                        } catch (NumberFormatException ignored) {}
                        break;
                    case "minvalue":
                        try {
                            minValue = Double.parseDouble(valStr);
                        } catch (NumberFormatException ignored) {}
                        break;
                    case "maxvalue":
                        try {
                            maxValue = Double.parseDouble(valStr);
                        } catch (NumberFormatException ignored) {}
                        break;
                }
            }

            if (minLength != null && maxLength != null && minLength > maxLength) {
                valid = false;
                context.buildConstraintViolationWithTemplate(
                                String.format("Minimum length (%d) cannot be greater than maximum length (%d) for field '%s'.",
                                        minLength, maxLength, fieldDisplayName))
                        .addPropertyNode("validations")
                        .addConstraintViolation();
            }

            if (minValue != null && maxValue != null && minValue > maxValue) {
                valid = false;
                context.buildConstraintViolationWithTemplate(
                                String.format("Minimum value (%s) cannot be greater than maximum value (%s) for field '%s'.",
                                        minValue, maxValue, fieldDisplayName))
                        .addPropertyNode("validations")
                        .addConstraintViolation();
            }
        }

        // Default value validation
        if (dto.getDefaultValue() == null || dto.getDefaultValue().isBlank())
            return valid;

        String trimmedDefault = dto.getDefaultValue().trim();

        // Date / DateTime checks
        if ("Date".equalsIgnoreCase(dto.getType())) {
            try {
                LocalDate.parse(trimmedDefault);
            } catch (DateTimeParseException e) {
                valid = false;
                context.buildConstraintViolationWithTemplate(
                                String.format(
                                        "Default value '%s' for field '%s' must be a valid date value.",
                                        trimmedDefault, fieldDisplayName))
                        .addPropertyNode("defaultValue")
                        .addConstraintViolation();
            }
        } else if ("DateTime".equalsIgnoreCase(dto.getType())) {
            try {
                LocalDateTime.parse(trimmedDefault);
            } catch (DateTimeParseException e) {
                valid = false;
                context.buildConstraintViolationWithTemplate(
                                String.format(
                                        "Default value '%s' for field '%s' must be a valid date and time value.",
                                        trimmedDefault, fieldDisplayName))
                        .addPropertyNode("defaultValue")
                        .addConstraintViolation();
            }
        }

        // Options validation for Dropdown / RadioButton
        if (dto.getOptions() != null && !dto.getOptions().isEmpty()
                && ("Dropdown".equalsIgnoreCase(dto.getType()) || "RadioButton".equalsIgnoreCase(dto.getType()))) {
            boolean match = dto.getOptions().stream()
                    .anyMatch(o -> o.getValue() != null && o.getValue().trim().equalsIgnoreCase(trimmedDefault));
            if (!match) {
                valid = false;
                context.buildConstraintViolationWithTemplate(
                                String.format(
                                        "Default value '%s' must be one of the defined options for field '%s'.",
                                        trimmedDefault, fieldDisplayName))
                        .addPropertyNode("defaultValue")
                        .addConstraintViolation();
            }
        }

        // Numeric and length validations
        if (dto.getValidations() != null) {
            for (FieldValidationDto v : dto.getValidations()) {
                String type = v.getType();
                if (type == null) {
                    continue;
                }
                switch (type.toLowerCase()) {
                    case "minvalue":
                        try {
                            double min = Double.parseDouble(v.getValue().toString());
                            double val = Double.parseDouble(trimmedDefault);
                            if (val < min) {
                                valid = false;
                                context.buildConstraintViolationWithTemplate(
                                                String.format(
                                                        "Default value '%s' for field '%s' cannot be less than minimum value %s.",
                                                        trimmedDefault, fieldDisplayName, min))
                                        .addPropertyNode("defaultValue")
                                        .addConstraintViolation();
                            }
                        } catch (NumberFormatException ignored) {
                        }
                        break;
                    case "maxvalue":
                        try {
                            double max = Double.parseDouble(v.getValue().toString());
                            double val = Double.parseDouble(trimmedDefault);
                            if (val > max) {
                                valid = false;
                                context.buildConstraintViolationWithTemplate(
                                                String.format(
                                                        "Default value '%s' for field '%s' cannot be greater than maximum value %s.",
                                                        trimmedDefault, fieldDisplayName, max))
                                        .addPropertyNode("defaultValue")
                                        .addConstraintViolation();
                            }
                        } catch (NumberFormatException ignored) {
                        }
                        break;
                    case "minlength":
                        try {
                            int minLen = Integer.parseInt(v.getValue().toString());
                            if (trimmedDefault.length() < minLen) {
                                valid = false;
                                context.buildConstraintViolationWithTemplate(
                                                String.format(
                                                        "Default value for field '%s' must have a minimum length of %d characters.",
                                                        fieldDisplayName, minLen))
                                        .addPropertyNode("defaultValue")
                                        .addConstraintViolation();
                            }
                        } catch (NumberFormatException ignored) {
                        }
                        break;
                    case "maxlength":
                        try {
                            int maxLen = Integer.parseInt(v.getValue().toString());
                            if (trimmedDefault.length() > maxLen) {
                                valid = false;
                                context.buildConstraintViolationWithTemplate(
                                                String.format(
                                                        "Default value for field '%s' cannot exceed maximum length of %d characters.",
                                                        fieldDisplayName, maxLen))
                                        .addPropertyNode("defaultValue")
                                        .addConstraintViolation();
                            }
                        } catch (NumberFormatException ignored) {
                        }
                        break;
                    default:
                        // No action for unknown validation types
                }
            }
        }
        
        return valid;
    }
}
