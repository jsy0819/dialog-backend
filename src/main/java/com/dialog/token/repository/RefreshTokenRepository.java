package com.dialog.token.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import com.dialog.token.domain.RefreshToken;
import com.dialog.user.domain.MeetUser;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByRefreshToken(String refreshToken);
    
    List<RefreshToken> findByUserId(Long userId);
    
    void deleteByExpiresAtBefore(LocalDateTime now); // 만료된 토큰 삭제용
    
    // 현재 로그인한 사용자의 Refresh 토큰 보유 유무 조회
    Optional<RefreshToken> findFirstByUserAndRevokedIsFalseAndExpiresAtAfter(MeetUser user, LocalDateTime now);
    
    // 리프레시 토큰도 삭제해야해서 메서드 추가.
    @Transactional
	void deleteByUser(MeetUser user);
    
    
}
