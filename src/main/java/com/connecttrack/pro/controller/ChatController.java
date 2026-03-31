package com.connecttrack.pro.controller;

import com.connecttrack.pro.dto.ChatMessage;
import com.connecttrack.pro.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // <-- Make sure this is imported
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    // --- NO ANNOTATION NEEDED ---
    // Open to all authenticated users.
    @GetMapping("/history")
    public ResponseEntity<List<ChatMessage>> getChatHistory() {
        return ResponseEntity.ok(chatService.getChatHistory());
    }

    // --- FIX: The DELETE endpoint uses method-level security ---
    // The endpoint itself requires authentication, and the service layer
    // handles the detailed business rules (Super Admin vs. Section Admin vs. Employee).
    @DeleteMapping("/messages/{id}")
    @PreAuthorize("isAuthenticated()") // Any authenticated user can ATTEMPT to delete.
    public ResponseEntity<?> deleteMessage(@PathVariable Long id) {
        try {
            chatService.deleteMessage(id);
            return ResponseEntity.ok().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}