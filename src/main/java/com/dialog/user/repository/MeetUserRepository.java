package com.dialog.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dialog.user.domain.MeetUser;

public interface MeetUserRepository extends JpaRepository<MeetUser, Long> {

    // email 컬럼을 기반으로 MeetUser 객체를 optional 형태로 조회
    Optional<MeetUser> findByEmail(String email);
    
//    // email 컬럼으로 Optional<MeetUser> 조회
//    Optional<MeetUser> findByEmail(String email);
 
    // 소셜 로그인 고유 ID 조회
    Optional<MeetUser> findBySnsId(String snsId);

    // email 컬럼이 DB에 존재하는지 여부 확인 (중복 체크 등에 활용)
    boolean existsByEmail(String email);
    
//    // email 컬럼 존재 여부 확인
//    boolean existsByEmail(String email);
}
