package com.example.aitools.service;

import com.example.aitools.model.ProviderType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class ProviderClient {
    private final RestClient restClient;

    @Value("${providers.gpt.base-url:https://api.openai.com/v1}")
    private String gptBaseUrl;

    @Value("${providers.gpt.api-key:YOUR_GPT_KEY}")
    private String gptApiKey;

    @Value("${providers.grok.base-url:https://api.x.ai/v1}")
    private String grokBaseUrl;

    @Value("${providers.grok.api-key:YOUR_GROK_KEY}")
    private String grokApiKey;

    @Value("${providers.deepseek.base-url:https://api.deepseek.com/v1}")
    private String deepseekBaseUrl;

    @Value("${providers.deepseek.api-key:YOUR_DEEPSEEK_KEY}")
    private String deepseekApiKey;

    public ProviderClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public String chat(ProviderType provider, String prompt, String context) {
        return callProvider(provider, prompt, context, false);
    }

    public String generateImage(ProviderType provider, String prompt) {
        return callProvider(provider, prompt, null, true);
    }

    private String callProvider(ProviderType provider, String prompt, String context, boolean image) {
        String baseUrl = switch (provider) {
            case GPT -> gptBaseUrl;
            case GROK -> grokBaseUrl;
            case DEEPSEEK -> deepseekBaseUrl;
        };
        String apiKey = switch (provider) {
            case GPT -> gptApiKey;
            case GROK -> grokApiKey;
            case DEEPSEEK -> deepseekApiKey;
        };

        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Authorization", "Bearer " + apiKey);
        headers.add("Content-Type", MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> payload = Map.of(
                "prompt", prompt,
                "context", context,
                "mode", image ? "image" : "chat"
        );

        return restClient.post()
                .uri(baseUrl + "/mock-endpoint")
                .headers(httpHeaders -> httpHeaders.addAll(headers))
                .body(payload)
                .retrieve()
                .body(String.class);
    }
}
