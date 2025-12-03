package com.example.aitools.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class StorageService {
    private final Path storagePath;

    public StorageService(@Value("${app.upload-dir:uploads}") String uploadDir) throws IOException {
        this.storagePath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(storagePath);
    }

    public String store(MultipartFile file) throws IOException {
        String filename = UUID.randomUUID() + "-" + StringUtils.cleanPath(file.getOriginalFilename());
        Path target = storagePath.resolve(filename);
        Files.copy(file.getInputStream(), target);
        return "/files/" + filename;
    }

    public Path load(String filename) {
        return storagePath.resolve(filename);
    }
}
