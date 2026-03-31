// File: src/main/java/com/connecttrack/pro/repository/NoticeRepository.java
package com.connecttrack.pro.repository;

import com.connecttrack.pro.entity.Notice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, Long> {
    // This new method will get all notices, but it will order them so that
    // pinned notices (is_pinned DESC) appear first, and then all notices
    // are sorted by the most recent date (date DESC).
    List<Notice> findAllByOrderByIsPinnedDescDateDesc();
}