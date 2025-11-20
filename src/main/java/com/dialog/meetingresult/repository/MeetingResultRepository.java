package com.dialog.meetingresult.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dialog.meetingresult.domain.MeetingResult;

public interface MeetingResultRepository extends JpaRepository<MeetingResult, Long> {
	// Meeting ID로 결과 조회 (1:1 관계이므로 Optional 반환)
	Optional<MeetingResult> findByMeetingId(Long meetingId);

	// Meeting ID로 결과 삭제
	void deleteByMeetingId(Long meetingId);
}