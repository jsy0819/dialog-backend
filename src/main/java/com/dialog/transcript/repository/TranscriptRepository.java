package com.dialog.transcript.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.dialog.transcript.domain.Transcript;

public interface TranscriptRepository extends JpaRepository<Transcript, Long> {

    // Meeting ID로 모든 Transcript 조회 (순서대로)
    @Query("SELECT t FROM Transcript t WHERE t.meeting.id = :meetingId ORDER BY t.sequenceOrder ASC")
    List<Transcript> findByMeetingIdOrderBySequenceOrder(@Param("meetingId") Long meetingId);
    
    // Meeting ID로 Transcript 존재 여부 확인
    boolean existsByMeetingId(Long meetingId);
    
    // Meeting ID로 Transcript 삭제
    void deleteByMeetingId(Long meetingId);
    
    // 특정 화자의 발화만 조회
    @Query("SELECT t FROM Transcript t WHERE t.meeting.id = :meetingId AND t.speakerId = :speakerId ORDER BY t.sequenceOrder ASC")
    List<Transcript> findByMeetingIdAndSpeakerId(@Param("meetingId") Long meetingId, @Param("speakerId") String speakerId);
}