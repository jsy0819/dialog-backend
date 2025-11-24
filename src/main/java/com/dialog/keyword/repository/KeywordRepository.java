package com.dialog.keyword.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import com.dialog.keyword.domain.Keyword;

public interface KeywordRepository extends JpaRepository<Keyword, Long> {
	// 이름으로 키워드 찾기 (MeetingService에서 사용 중)
	Optional<Keyword> findByName(String name);
	
	// MeetingResultKeyword 테이블에 자신의 ID가 없는 Keyword를 찾아서 삭제합니다.
    @Modifying
    @Transactional
    @Query("DELETE FROM Keyword k WHERE NOT EXISTS (SELECT 1 FROM MeetingResultKeyword mrk WHERE mrk.keyword = k)")
    void deleteOrphanKeywords();
}