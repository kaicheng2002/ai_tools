package com.example.aitools.service;

import com.example.aitools.dto.ImageRequest;
import org.springframework.stereotype.Service;

@Service
public class ImageService {
    private final ProviderClient providerClient;

    public ImageService(ProviderClient providerClient) {
        this.providerClient = providerClient;
    }

    public String generate(ImageRequest request) {
        return providerClient.generateImage(request.getProvider(), request.getPrompt());
    }
}
