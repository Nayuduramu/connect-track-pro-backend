package com.connecttrack.pro.service;

import com.connecttrack.pro.dto.ChatMessage;
import com.connecttrack.pro.entity.Employee;
import com.connecttrack.pro.repository.ChatMessageRepository;
import com.connecttrack.pro.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class ChatService {

    @Autowired private ChatMessageRepository chatMessageRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private SimpMessagingTemplate messagingTemplate;

    public List<ChatMessage> getChatHistory() {
        List<com.connecttrack.pro.entity.ChatMessage> messages =
                chatMessageRepository.findTop50ByOrderByTimestampDesc();
        return messages.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    public void deleteMessage(Long messageId) {
        Employee currentUser = getCurrentUser();
        com.connecttrack.pro.entity.ChatMessage messageToDelete = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found with id: " + messageId));
        
        Employee messageSender = messageToDelete.getSender();
        
        // --- CORRECTED SECURITY LOGIC ---
        String currentUserRole = currentUser.getRole().getName();
        String messageSenderRole = messageSender.getRole().getName();
        boolean isAllowedToDelete = false;

        // Rule 1: Anyone can delete their own messages.
        if (currentUser.getId().equals(messageSender.getId())) {
            isAllowedToDelete = true;
        } 
        // Rule 2: Super Admin can delete ANY message.
        else if ("ROLE_SUPER_ADMIN".equals(currentUserRole)) {
            isAllowedToDelete = true;
        } 
        // Rule 3: Section Admin logic.
        else if ("ROLE_ADMIN".equals(currentUserRole) || "ROLE_SECTION_ADMIN".equals(currentUserRole)) {
            // A Section Admin cannot delete a Super Admin's message.
            if (!"ROLE_SUPER_ADMIN".equals(messageSenderRole)) {
                isAllowedToDelete = true;
            }
        }

        if (isAllowedToDelete) {
            chatMessageRepository.deleteById(messageId);
            Map<String, Object> deleteNotification = new HashMap<>();
            deleteNotification.put("deletedMessageId", messageId);
            messagingTemplate.convertAndSend("/topic/public.delete", deleteNotification);
        } else {
            throw new SecurityException("You are not authorized to delete this message.");
        }
    }
    
    private Employee getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();
        return employeeRepository.findByEmail(userEmail)
            .orElseThrow(() -> new RuntimeException("Authenticated user not found."));
    }

    private ChatMessage convertToDto(com.connecttrack.pro.entity.ChatMessage entity) {
        ChatMessage dto = new ChatMessage();
        dto.setId(entity.getId());
        dto.setContent(entity.getContent());
        dto.setSender(entity.getSenderName());
        dto.setType(ChatMessage.MessageType.valueOf(entity.getType()));
        dto.setTimestamp(entity.getTimestamp());
        
        if (entity.getSender() != null) {
            dto.setSenderRole(entity.getSender().getRole().getName());
        }
        return dto;
    }
}