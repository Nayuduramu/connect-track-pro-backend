// File: src/main/java/com/connecttrack/pro/repository/HolidayRepository.java
package com.connecttrack.pro.repository;

import com.connecttrack.pro.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// --- THESE ARE THE FIXES ---
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
// --- END OF FIXES ---

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, Long> {
    
    List<Holiday> findAllByOrderByHolidayDateAsc();
    
    // This method will now compile correctly
    Optional<Holiday> findByHolidayDate(LocalDate date);
}