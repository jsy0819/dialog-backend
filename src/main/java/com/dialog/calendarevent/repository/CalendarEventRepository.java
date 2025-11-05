package com.dialog.calendarevent.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dialog.calendarevent.domain.CalendarEvent;
import com.dialog.user.domain.MeetUser;

public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {

	List<CalendarEvent> findByUserIdAndEventDateBetween(Long userId, LocalDate startDate, LocalDate endDate);

	Optional<CalendarEvent> findByGoogleEventIdAndUserId(String eventId, Long id);
}
