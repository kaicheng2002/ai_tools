package com.example.aitools.controller;

import com.example.aitools.dto.ChatRequest;
import com.example.aitools.model.Conversation;
import com.example.aitools.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin
public class ChatController {
    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public Conversation chat(@Valid @RequestBody ChatRequest request) {
        return chatService.chat(request);
    }

    @GetMapping("/{id}")
    public Conversation get(@PathVariable Long id) {
        return chatService.getConversation(id);
    }
}
