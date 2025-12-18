package com.example.aitools.controller;

import com.example.aitools.dto.ImageRequest;
import com.example.aitools.service.ImageService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/images")
@CrossOrigin
public class ImageController {
    private final ImageService imageService;

    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    @PostMapping
    public String generate(@Valid @RequestBody ImageRequest request) {
        return imageService.generate(request);
    }
}
