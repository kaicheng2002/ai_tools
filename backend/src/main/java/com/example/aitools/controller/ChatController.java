package com.example.aitools.controller;

import com.example.aitools.dto.ChatRequest;
import com.example.aitools.dto.ConversationView;
import com.example.aitools.model.Conversation;
import com.example.aitools.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin
public class ChatController {
    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/conversations")
    public List<Conversation> list() {
        return chatService.listConversations();
    }

    @GetMapping("/{id}")
    public ConversationView get(@PathVariable Long id) {
        return chatService.getConversation(id);
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@Valid ChatRequest request) {
        return chatService.streamChat(request);
    }
}
