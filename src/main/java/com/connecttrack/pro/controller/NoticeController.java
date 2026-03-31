// 📄 File: src/main/java/com/connecttrack/pro/controller/NoticeController.java

package com.connecttrack.pro.controller;

import com.connecttrack.pro.dto.CreateNoticeRequest;
import com.connecttrack.pro.dto.NoticeDTO;
import com.connecttrack.pro.dto.UpdateNoticeRequest;
import com.connecttrack.pro.entity.Employee;
import com.connecttrack.pro.entity.Notice;
import com.connecttrack.pro.mapper.NoticeMapper;
import com.connecttrack.pro.repository.EmployeeRepository;
import com.connecttrack.pro.repository.NoticeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/notices")
public class NoticeController {

    @Autowired
    private NoticeRepository noticeRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private NoticeMapper noticeMapper;

    // ------------------------------------------------------------
    // ✅ GET: Fetch all notices (pinned first, then most recent)
    // ------------------------------------------------------------
    @GetMapping
    public ResponseEntity<List<NoticeDTO>> getAllNotices() {
        List<Notice> notices = noticeRepository.findAllByOrderByIsPinnedDescDateDesc();
        List<NoticeDTO> noticeDTOs = notices.stream()
                .map(noticeMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(noticeDTOs);
    }

    // ------------------------------------------------------------
    // ✅ POST: Create a new notice (Admin / Super Admin only)
    // ------------------------------------------------------------
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<NoticeDTO> createNotice(@RequestBody CreateNoticeRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();

        Employee author = employeeRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Author not found"));

        Notice notice = new Notice();
        notice.setTitle(request.getTitle());
        notice.setContent(request.getContent());
        notice.setAuthor(author.getFullName());
        notice.setDate(LocalDateTime.now());
        notice.setPinned(false);

        Notice savedNotice = noticeRepository.save(notice);
        return ResponseEntity.ok(noticeMapper.toDto(savedNotice));
    }

    // ------------------------------------------------------------
    // ✅ PUT: Update (Edit) an existing notice (Admin / Super Admin)
    // ------------------------------------------------------------
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<NoticeDTO> updateNotice(
            @PathVariable Long id,
            @RequestBody UpdateNoticeRequest request) {

        return noticeRepository.findById(id)
                .map(notice -> {
                    notice.setTitle(request.getTitle());
                    notice.setContent(request.getContent());
                    Notice updatedNotice = noticeRepository.save(notice);
                    return ResponseEntity.ok(noticeMapper.toDto(updatedNotice));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ------------------------------------------------------------
    // ✅ DELETE: Remove a notice (Admin / Super Admin only)
    // ------------------------------------------------------------
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<Void> deleteNotice(@PathVariable Long id) {
        if (!noticeRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        noticeRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    // ------------------------------------------------------------
    // ✅ POST: Toggle pin/unpin a notice (Admin / Super Admin only)
    // ------------------------------------------------------------
    @PostMapping("/{id}/pin")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<NoticeDTO> togglePinStatus(@PathVariable Long id) {
        return noticeRepository.findById(id)
                .map(notice -> {
                    notice.setPinned(!notice.isPinned());
                    Notice updatedNotice = noticeRepository.save(notice);
                    return ResponseEntity.ok(noticeMapper.toDto(updatedNotice));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
