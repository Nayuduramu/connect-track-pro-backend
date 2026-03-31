// File: src/main/java/com/connecttrack/pro/service/HolidayService.java
package com.connecttrack.pro.service;

import com.connecttrack.pro.entity.Holiday;
import com.connecttrack.pro.repository.HolidayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class HolidayService {

    private final HolidayRepository holidayRepository;

    // -----------------------------------------------------------
    // ✅ Fetch ALL stored holidays — (NO auto weekend generation)
    // -----------------------------------------------------------
    public List<Holiday> getAllHolidays() {
        return holidayRepository.findAllByOrderByHolidayDateAsc();
    }

    // -----------------------------------------------------------
    // ✅ Add OR Update Holiday
    // -----------------------------------------------------------
    public Holiday addHoliday(Holiday newHolidayDetails) {

        Optional<Holiday> existingHolidayOptional =
                holidayRepository.findByHolidayDate(newHolidayDetails.getHolidayDate());

        Holiday holidayToSave;

        if (existingHolidayOptional.isPresent()) {
            // Case 1: Holiday already exists — update name only
            holidayToSave = existingHolidayOptional.get();
            holidayToSave.setName(newHolidayDetails.getName());

        } else {
            // Case 2: Create a new holiday entry
            holidayToSave = newHolidayDetails;

            DayOfWeek day = holidayToSave.getHolidayDate().getDayOfWeek();

            if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
                holidayToSave.setHolidayType(Holiday.HolidayType.WEEKEND_OVERRIDE);
            } else {
                holidayToSave.setHolidayType(Holiday.HolidayType.NATIONAL);
            }
        }

        return holidayRepository.save(holidayToSave);
    }

    // -----------------------------------------------------------
    // ❌ Delete Holiday (national or weekend override)
    // -----------------------------------------------------------
    public void deleteHoliday(Long id) {
        if (holidayRepository.existsById(id)) {
            holidayRepository.deleteById(id);
        }
    }

    // -----------------------------------------------------------
    // 🔁 Toggle Weekend WORKING / NON-WORKING
    // -----------------------------------------------------------
    public void toggleWeekendWorkingStatus(LocalDate date, boolean isNowWorking) {

        DayOfWeek dow = date.getDayOfWeek();
        if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
            throw new IllegalArgumentException(
                "Weekend toggling is only allowed for Saturdays or Sundays."
            );
        }

        Optional<Holiday> existingHoliday = holidayRepository.findByHolidayDate(date);

        if (isNowWorking) {
            // MARK WEEKEND AS WORKING → delete override entry if exists
            if (existingHoliday.isPresent()) {
                holidayRepository.delete(existingHoliday.get());
                holidayRepository.flush(); // ensures immediate sync
            }

        } else {
            // MARK WEEKEND AS NON-WORKING → ensure override entry exists
            if (existingHoliday.isEmpty()) {
                Holiday weekendHoliday = new Holiday();
                weekendHoliday.setName("Weekend");
                weekendHoliday.setHolidayDate(date);
                weekendHoliday.setHolidayType(Holiday.HolidayType.WEEKEND_OVERRIDE);

                holidayRepository.saveAndFlush(weekendHoliday);
            }
        }
    }
}
