// File: src/main/java/com/connecttrack/pro/mapper/NoticeMapper.java
package com.connecttrack.pro.mapper;

import com.connecttrack.pro.dto.NoticeDTO;
import com.connecttrack.pro.entity.Notice;
import org.springframework.stereotype.Component;

@Component
public class NoticeMapper {

    /**
     * Converts a Notice database entity to a NoticeDTO.
     * The DTO is what gets sent to the Flutter app via the API.
     * @param notice The entity object retrieved from the database.
     * @return The DTO that will be serialized into JSON.
     */
    public NoticeDTO toDto(Notice notice) {
        if (notice == null) {
            return null;
        }

        NoticeDTO dto = new NoticeDTO();
        
        // Map all the fields from the entity to the DTO
        dto.setId(notice.getId());
        dto.setTitle(notice.getTitle());
        dto.setContent(notice.getContent());
        dto.setAuthor(notice.getAuthor());
        dto.setDate(notice.getDate());
        dto.setPinned(notice.isPinned()); // Use the 'isPinned' getter for boolean

        return dto;
    }
}