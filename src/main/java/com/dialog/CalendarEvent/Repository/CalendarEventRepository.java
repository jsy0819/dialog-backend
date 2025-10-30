package com.dialog.CalendarEvent.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dialog.CalendarEvent_.CalendarEvent;

public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {

	List<CalendarEvent> findByUserIdAndEventDateBetween(Long userId, LocalDate startDate, LocalDate endDate);
}
