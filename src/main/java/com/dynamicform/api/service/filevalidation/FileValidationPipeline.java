package com.dynamicform.api.service.filevalidation;

import com.dynamicform.api.entity.FormFieldEntity;
import org.springframework.web.multipart.MultipartFile;

public interface FileValidationPipeline {
    void validate(MultipartFile file, FormFieldEntity field);
}
