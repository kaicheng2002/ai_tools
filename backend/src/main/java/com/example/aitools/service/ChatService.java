package com.example.aitools.service;

import com.example.aitools.dto.ChatRequest;
import com.example.aitools.model.Conversation;
import com.example.aitools.model.Message;
import com.example.aitools.model.ProviderType;
import com.example.aitools.repository.ConversationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        conversation.addMessage(userMessage);

        String history = conversation.getMessages().stream()
                .map(Message::getContent)
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");

        String answer = providerClient.chat(request.getProvider(), request.getPrompt(), history);

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
