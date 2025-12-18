package com.example.aitools.service;

import com.example.aitools.model.ProviderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class ProviderClient {
    private static final Logger log = LoggerFactory.getLogger(ProviderClient.class);

    private final RestClient restClient;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${providers.gpt.base-url:https://api.openai.com/v1}")
    private String gptBaseUrl;

    @Value("${providers.gpt.api-key:YOUR_GPT_KEY}")
    private String gptApiKey;

    @Value("${providers.gpt.model:gpt-4o-mini}")
    private String gptModel;

    @Value("${providers.gpt.chat-path:/chat/completions}")
    private String gptChatPath;

    @Value("${providers.gpt.image-path:/images/generations}")
    private String gptImagePath;

    @Value("${providers.gpt.mock:true}")
    private boolean gptMock;

    @Value("${providers.grok.base-url:https://api.x.ai/v1}")
    private String grokBaseUrl;

    @Value("${providers.grok.api-key:YOUR_GROK_KEY}")
    private String grokApiKey;

    @Value("${providers.grok.model:grok-beta}")
    private String grokModel;

    @Value("${providers.grok.chat-path:/chat/completions}")
    private String grokChatPath;

    @Value("${providers.grok.image-path:/images/generations}")
    private String grokImagePath;

    @Value("${providers.grok.mock:true}")
    private boolean grokMock;

    @Value("${providers.deepseek.base-url:https://api.deepseek.com/v1}")
    private String deepseekBaseUrl;

    @Value("${providers.deepseek.api-key:YOUR_DEEPSEEK_KEY}")
    private String deepseekApiKey;

    @Value("${providers.deepseek.model:deepseek-chat}")
    private String deepseekModel;

    @Value("${providers.deepseek.chat-path:/chat/completions}")
    private String deepseekChatPath;

    @Value("${providers.deepseek.image-path:/images/generations}")
    private String deepseekImagePath;

    @Value("${providers.deepseek.mock:true}")
    private boolean deepseekMock;

    public ProviderClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public String chat(ProviderType provider, String prompt, String context) {
        ProviderSettings settings = settings(provider);
        if (settings.mock()) {
            return mockChat(provider, prompt, context);
        }
        try {
            String raw = restClient.post()
                    .uri(settings.baseUrl() + settings.chatPath())
                    .headers(headers -> headers.addAll(defaultHeaders(settings.apiKey())))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(chatPayload(settings.model(), prompt, context))
                    .retrieve()
                    .body(String.class);
            return normalizeAnswer(raw);
        } catch (Exception ex) {
            log.warn("Provider call failed, falling back to mock: {}", ex.getMessage());
            return mockChat(provider, prompt, context);
        }
    }

    public List<String> streamChat(ProviderType provider, String prompt, String context) {
        String answer = chat(provider, prompt, context);
        return Arrays.asList(answer.split("(?<=\\G.{12})"));
    }

    public String generateImage(ProviderType provider, String prompt) {
        ProviderSettings settings = settings(provider);
        if (settings.mock()) {
            return mockImage(provider, prompt);
        }
        try {
            return restClient.post()
                    .uri(settings.baseUrl() + settings.imagePath())
                    .headers(headers -> headers.addAll(defaultHeaders(settings.apiKey())))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(imagePayload(settings.model(), prompt))
                    .retrieve()
                    .body(String.class);
        } catch (Exception ex) {
            log.warn("Image provider call failed, falling back to mock: {}", ex.getMessage());
            return mockImage(provider, prompt);
        }
    }

    private ProviderSettings settings(ProviderType provider) {
        return switch (provider) {
            case GPT -> new ProviderSettings(gptBaseUrl, gptApiKey, gptModel, gptChatPath, gptImagePath, gptMock);
            case GROK -> new ProviderSettings(grokBaseUrl, grokApiKey, grokModel, grokChatPath, grokImagePath, grokMock);
            case DEEPSEEK -> new ProviderSettings(deepseekBaseUrl, deepseekApiKey, deepseekModel, deepseekChatPath, deepseekImagePath, deepseekMock);
        };
    }

    private MultiValueMap<String, String> defaultHeaders(String apiKey) {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Authorization", "Bearer " + apiKey);
        headers.add("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        return headers;
    }

    private Map<String, Object> chatPayload(String model, String prompt, String context) {
        return Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", "You are a helpful AI assistant." + (context != null && !context.isBlank() ? " Context:" + context : "")),
                        Map.of("role", "user", "content", prompt)
                )
        );
    }

    private Map<String, Object> imagePayload(String model, String prompt) {
        return Map.of(
                "model", model,
                "prompt", prompt,
                "size", "1024x1024"
        );
    }

    private String mockChat(ProviderType provider, String prompt, String context) {
        StringBuilder builder = new StringBuilder();
        builder.append("【").append(provider.name()).append(" 模拟回复】").append(prompt);
        if (context != null && !context.isBlank()) {
            builder.append("\n\n(参考记忆：").append(summary(context)).append(")");
        }
        return builder.toString();
    }

    private String mockImage(ProviderType provider, String prompt) {
        return "mock://" + provider.name().toLowerCase() + "/image?prompt=" + prompt.replaceAll("\\s+", "+") + "&ts=" + Instant.now().toEpochMilli();
    }

    private String summary(String text) {
        int max = 180;
        if (text.length() <= max) {
            return text;
        }
        return text.substring(0, max) + "...";
    }

    private String normalizeAnswer(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        try {
            JsonNode root = mapper.readTree(raw);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && !choices.isEmpty()) {
                JsonNode first = choices.get(0);
                JsonNode message = first.path("message");
                if (message.has("content")) {
                    return message.get("content").asText();
                }
                JsonNode delta = first.path("delta");
                if (delta.has("content")) {
                    return delta.get("content").asText();
                }
                if (first.has("text")) {
                    return first.get("text").asText();
                }
            }
        } catch (Exception e) {
            log.debug("Failed to parse provider response, return raw: {}", e.getMessage());
        }
        return raw;
    }

    private record ProviderSettings(String baseUrl, String apiKey, String model, String chatPath, String imagePath,
                                    boolean mock) {
    }
}
