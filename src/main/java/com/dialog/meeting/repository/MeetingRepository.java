package com.dialog.meeting.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dialog.meeting.domain.Meeting;
import com.dialog.user.domain.MeetUser;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {

	void deleteByHostUser(MeetUser user);
	
	// 전체 회의수 반환
	long count(); 
	
	// 이번 달 회의 생성 개수
	@Query(value = "SELECT COUNT(*) FROM meeting WHERE created_at BETWEEN :start AND :end", nativeQuery = true)
	long countMeetingsInMonth(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

	// 오늘 생성한 회의 수
	@Query(value = "SELECT COUNT(*) FROM meeting WHERE created_at BETWEEN :start AND :end", nativeQuery = true)
	long countTodayCreatedMeetings(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

	// 어제 생성한 회의 수
	@Query(value = "SELECT COUNT(*) FROM meeting WHERE created_at BETWEEN :start AND :end", nativeQuery = true)
	long countYesterdayCreatedMeetings(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

	List<Meeting> findAllByScheduledAtBetween(LocalDateTime startDateTime, LocalDateTime endDateTime);	
	
}