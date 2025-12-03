package com.example.aitools.service;

import com.example.aitools.dto.ChatRequest;
import com.example.aitools.model.Conversation;
import com.example.aitools.model.Message;
import com.example.aitools.model.ProviderType;
import com.example.aitools.repository.ConversationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
public class ChatService {
    private final ConversationRepository conversationRepository;
    private final ProviderClient providerClient;

    public ChatService(ConversationRepository conversationRepository, ProviderClient providerClient) {
        this.conversationRepository = conversationRepository;
        this.providerClient = providerClient;
    }

    @Transactional
    public Conversation chat(ChatRequest request) {
        Conversation conversation = request.getConversationId() != null
                ? conversationRepository.findById(request.getConversationId()).orElseGet(Conversation::new)
                : new Conversation();

        Message userMessage = new Message();
        userMessage.setRole("user");
        userMessage.setContent(request.getPrompt());
        if (request.getFileUrls() != null && !request.getFileUrls().isEmpty()) {
            userMessage.setAttachments(String.join("\n", request.getFileUrls()));
        }
        conversation.addMessage(userMessage);

        String history = conversation.getMessages().stream()
                .map(msg -> msg.getRole() + ": " + msg.getContent() +
                        (msg.getAttachments() != null ? "\n附件:\n" + msg.getAttachments() : ""))
                .collect(Collectors.joining("\n\n"));

        String promptWithFiles = request.getPrompt();
        if (request.getFileUrls() != null && !request.getFileUrls().isEmpty()) {
            promptWithFiles += "\n\n附件地址:\n" + String.join("\n", request.getFileUrls());
        }

        String answer = providerClient.chat(request.getProvider(), promptWithFiles, history);

        Message aiMessage = new Message();
        aiMessage.setRole("assistant");
        aiMessage.setProvider(request.getProvider());
        aiMessage.setContent(answer);
        conversation.addMessage(aiMessage);

        if (conversation.getTitle() == null) {
            conversation.setTitle(request.getPrompt().substring(0, Math.min(30, request.getPrompt().length())));
        }

        return conversationRepository.save(conversation);
    }

    @Transactional(readOnly = true)
    public Conversation getConversation(Long id) {
        return conversationRepository.findById(id).orElseThrow();
    }
}
