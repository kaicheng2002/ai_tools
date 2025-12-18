package com.example.aitools.dto;

import com.example.aitools.model.Conversation;
import com.example.aitools.model.Message;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationView {
    private Conversation conversation;
    private List<Message> messages;
}
