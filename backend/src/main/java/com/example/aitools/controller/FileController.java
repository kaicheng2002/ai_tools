package com.example.aitools.controller;

import com.example.aitools.service.StorageService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/files")
@CrossOrigin
public class FileController {
    private final StorageService storageService;

    public FileController(StorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String upload(@RequestParam("file") MultipartFile file) throws IOException {
        return storageService.store(file);
    }

    @GetMapping("/{filename}")
    public ResponseEntity<Resource> serve(@PathVariable String filename) throws IOException {
        Path file = storageService.load(filename);
        Resource resource = new UrlResource(file.toUri());
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).body(resource);
    }
}
