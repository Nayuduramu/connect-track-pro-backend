// File: src/main/java/com/connecttrack/pro/repository/ChatMessageRepository.java
package com.connecttrack.pro.repository;

import com.connecttrack.pro.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // This custom query will fetch the most recent 50 messages, ordered by time.
    List<ChatMessage> findTop50ByOrderByTimestampDesc();
}