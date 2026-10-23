package com.dynamicform.api.service.filevalidation;

import com.dynamicform.api.entity.FormFieldEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FileValidationPipelineImpl implements FileValidationPipeline {

    private final List<FileValidationRule> rules;

    @Override
    public void validate(MultipartFile file, FormFieldEntity field) {
        for (FileValidationRule rule : rules) {
            rule.validate(file, field);
        }
    }
}
