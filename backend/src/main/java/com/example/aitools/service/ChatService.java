package com.example.aitools.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aitools.dto.ChatRequest;
import com.example.aitools.dto.ConversationView;
import com.example.aitools.mapper.ConversationMapper;
import com.example.aitools.mapper.MessageMapper;
import com.example.aitools.model.Conversation;
import com.example.aitools.model.Message;
import com.example.aitools.model.ProviderType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
public class ChatService {
    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;
    private final ProviderClient providerClient;

    public ChatService(ConversationMapper conversationMapper, MessageMapper messageMapper, ProviderClient providerClient) {
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
        this.providerClient = providerClient;
    }

    public List<Conversation> listConversations() {
        return conversationMapper.selectList(new LambdaQueryWrapper<Conversation>()
                .orderByDesc(Conversation::getUpdatedAt));
    }

    public ConversationView getConversation(Long id) {
        Conversation conversation = conversationMapper.selectById(id);
        if (conversation == null) {
            throw new IllegalArgumentException("Conversation not found");
        }
        List<Message> messages = messageMapper.selectList(new LambdaQueryWrapper<Message>()
                .eq(Message::getConversationId, id)
                .orderByAsc(Message::getCreatedAt));
        return new ConversationView(conversation, messages);
    }

    @Transactional
    public Conversation saveUserMessage(ChatRequest request) {
        Conversation conversation = request.getConversationId() != null
                ? conversationMapper.selectById(request.getConversationId())
                : null;
        if (conversation == null) {
            conversation = new Conversation();
            conversation.setTitle(buildTitle(request.getPrompt()));
            conversationMapper.insert(conversation);
        }

        Message userMessage = new Message();
        userMessage.setConversationId(conversation.getId());
        userMessage.setRole("user");
        userMessage.setContent(request.getPrompt());
        if (!CollectionUtils.isEmpty(request.getFileUrls())) {
            userMessage.setAttachments(String.join("\n", request.getFileUrls()));
        }
        messageMapper.insert(userMessage);
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationMapper.updateById(conversation);
        return conversation;
    }

    public SseEmitter streamChat(ChatRequest request) {
        Conversation conversation = saveUserMessage(request);
        String context = buildContext(conversation.getId());
        String promptWithAttachments = mergePromptWithFiles(request.getPrompt(), request.getFileUrls());

        SseEmitter emitter = new SseEmitter(0L);
        AtomicReference<StringBuilder> aiBuffer = new AtomicReference<>(new StringBuilder());

        CompletableFuture.runAsync(() -> {
            try {
                emitter.send(SseEmitter.event().name("meta").data("{\"conversationId\":" + conversation.getId() + "}"));
                for (String chunk : providerClient.streamChat(request.getProvider(), promptWithAttachments, context)) {
                    aiBuffer.get().append(chunk);
                    emitter.send(SseEmitter.event().name("chunk").data(chunk));
                    Thread.sleep(60L);
                }
                persistAiAnswer(conversation.getId(), request.getProvider(), aiBuffer.get().toString());
                emitter.send(SseEmitter.event().name("done").data("done"));
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    private void persistAiAnswer(Long conversationId, ProviderType provider, String answer) {
        if (answer == null || answer.isBlank()) {
            return;
        }
        Message aiMessage = new Message();
        aiMessage.setConversationId(conversationId);
        aiMessage.setRole("assistant");
        aiMessage.setProvider(provider);
        aiMessage.setContent(answer);
        messageMapper.insert(aiMessage);
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation != null) {
            conversation.setUpdatedAt(LocalDateTime.now());
            conversationMapper.updateById(conversation);
        }
    }

    private String buildContext(Long conversationId) {
        List<Message> messages = messageMapper.selectList(new LambdaQueryWrapper<Message>()
                .eq(Message::getConversationId, conversationId)
                .orderByAsc(Message::getCreatedAt));
        return messages.stream()
                .map(msg -> msg.getRole() + ": " + msg.getContent() +
                        (msg.getAttachments() != null ? "\n附件:\n" + msg.getAttachments() : ""))
                .collect(Collectors.joining("\n\n"));
    }

    private String mergePromptWithFiles(String prompt, List<String> files) {
        if (CollectionUtils.isEmpty(files)) {
            return prompt;
        }
        return prompt + "\n\n附件地址:\n" + String.join("\n", files);
    }

    private String buildTitle(String prompt) {
        String clean = prompt.replaceAll("\n", " ").trim();
        return clean.length() > 30 ? clean.substring(0, 30) + "..." : clean;
    }
}
