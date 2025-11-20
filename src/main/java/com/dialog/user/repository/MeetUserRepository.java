package com.dialog.user.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dialog.user.domain.MeetUser;

public interface MeetUserRepository extends JpaRepository<MeetUser, Long> {

    // email 컬럼을 기반으로 MeetUser 객체를 optional 형태로 조회
    Optional<MeetUser> findByEmail(String email);
    
    // name 컬럼을 기반으로 MeetUser 객체를 optional 형태로 조회
    Optional<MeetUser> findByName(String name);

    // 소셜 로그인 고유 ID 조회
    Optional<MeetUser> findBySnsId(String snsId);

    boolean existsByEmail(String email);

    
    // 전체 가입자 수 (쿼리)
    long count();

    // 최근 7일간 가입자 수
    int countByCreatedAtAfter(LocalDateTime date);
    
    // 오늘 하루 가입자 수 - 범위 검색 (00:00:00 ~ 23:59:59)
    @Query(value = "SELECT COUNT(*) FROM user WHERE created_at BETWEEN :start AND :end", nativeQuery = true)
    long countTodayRegisteredUsers(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // 어제 가입자 수 - 범위 검색
    @Query(value = "SELECT COUNT(*) FROM user WHERE created_at BETWEEN :start AND :end", nativeQuery = true)
    long countYesterdayRegisteredUsers(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

	Optional<MeetUser> findByResetPasswordToken(String resetPasswordToken);
    
}
