package com.connecttrack.pro.controller;

import com.connecttrack.pro.dto.ChatMessage;
import com.connecttrack.pro.entity.Employee;
import com.connecttrack.pro.repository.ChatMessageRepository;
import com.connecttrack.pro.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime; // <-- NEW IMPORT

@Controller
public class WebSocketController {

    @Autowired
    private ChatMessageRepository chatMessageRepository;
    @Autowired
    private EmployeeRepository employeeRepository;

    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/public")
    public ChatMessage sendMessage(@Payload ChatMessage chatMessage) {
        // --- THIS IS THE FIX ---
        // Set the current server time on the message before broadcasting and saving.
        chatMessage.setTimestamp(LocalDateTime.now());

        if (ChatMessage.MessageType.CHAT.equals(chatMessage.getType())) {
            saveChatMessage(chatMessage);
        }
        return chatMessage;
    }

    @MessageMapping("/chat.addUser")
    @SendTo("/topic/public")
    public ChatMessage addUser(@Payload ChatMessage chatMessage, SimpMessageHeaderAccessor headerAccessor) {
        headerAccessor.getSessionAttributes().put("username", chatMessage.getSender());
        // Also set the timestamp for JOIN messages for consistency
        chatMessage.setTimestamp(LocalDateTime.now());
        return chatMessage;
    }

    private void saveChatMessage(ChatMessage dto) {
        Employee sender = employeeRepository.findByFullName(dto.getSender()).orElse(null);

        com.connecttrack.pro.entity.ChatMessage messageEntity = new com.connecttrack.pro.entity.ChatMessage();
        messageEntity.setSenderName(dto.getSender());
        messageEntity.setContent(dto.getContent());
        messageEntity.setType(dto.getType().name());
        messageEntity.setTimestamp(dto.getTimestamp()); // Save the timestamp to the database
        if (sender != null) {
            messageEntity.setSender(sender);
        }
        
        // The save method now returns the saved entity with its generated ID
        com.connecttrack.pro.entity.ChatMessage savedEntity = chatMessageRepository.save(messageEntity);
        // Set the ID on the DTO so the client who sent the message knows the ID
        dto.setId(savedEntity.getId());
    }
}