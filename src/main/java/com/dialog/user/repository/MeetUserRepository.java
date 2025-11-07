package com.dialog.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dialog.user.domain.MeetUser;

public interface MeetUserRepository extends JpaRepository<MeetUser, Long> {

    // email 컬럼을 기반으로 MeetUser 객체를 optional 형태로 조회
    Optional<MeetUser> findByEmail(String email);

    // 소셜 로그인 고유 ID 조회
    Optional<MeetUser> findBySnsId(String snsId);

    boolean existsByEmail(String email);

}
