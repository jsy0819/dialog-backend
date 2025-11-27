package com.dialog.recording.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dialog.recording.domain.Recording;

public interface RecordingRepository extends JpaRepository<Recording, Long> {

	// Meeting ID로 Recording 조회
    Optional<Recording> findByMeetingId(Long meetingId);
    
	// Meeting ID로 Recording 존재 여부 확인
    boolean existsByMeetingId(Long meetingId);
    
	// Audio File URL로 Recording 조회
    Optional<Recording> findByAudioFileUrl(String audioFileUrl);
    
	// Meeting ID로 Recording 삭제
    void deleteByMeetingId(Long meetingId);
}