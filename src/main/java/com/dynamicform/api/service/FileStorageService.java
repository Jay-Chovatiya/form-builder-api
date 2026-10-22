package com.dynamicform.api.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    String uploadFieldFile(String formCode, String fieldName, MultipartFile file);

    Resource loadFileAsResource(String formCode, String fileName);

    String getFileContentType(String formCode, String fileName);

    boolean removeFile(String formCode, String fileName);
}
