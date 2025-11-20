package com.dialog.keyword.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dialog.keyword.domain.Keyword;

public interface KeywordRepository extends JpaRepository<Keyword, Long> {
	// 이름으로 키워드 찾기 (MeetingService에서 사용 중)
	Optional<Keyword> findByName(String name);
}