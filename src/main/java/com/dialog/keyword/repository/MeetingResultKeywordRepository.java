package com.dialog.keyword.repository;

import com.dialog.keyword.domain.MeetingResultKeyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

public interface MeetingResultKeywordRepository extends JpaRepository<MeetingResultKeyword, Long> {
	
	// 'saveMeetingResultData'가 '먼저 삭제'할 수 있도록 이 메서드를 추가합니다.
    @Modifying
    @Transactional
    void deleteByMeetingResultId(Long meetingResultId);
}