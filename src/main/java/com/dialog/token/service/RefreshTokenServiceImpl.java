package com.dialog.token.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dialog.exception.RefreshTokenException;
import com.dialog.token.domain.RefreshToken;
import com.dialog.token.domain.RefreshTokenDto;
import com.dialog.token.repository.RefreshTokenRepository;
import com.dialog.user.domain.MeetUser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenServiceImpl implements RefreshTokenService {

    // JPA 리파지토리를 통해 DB의 refresh_token 테이블에 접근
    private final RefreshTokenRepository refreshTokenRepository;
    
    // 리프레시 토큰 기본 유효기간
    private final long refreshTokenDurationMs = 7 * 24 * 60 * 60 * 1000L; // 7일

    // 1. 리프레시 토큰 생성
    @Override
    public RefreshToken createRefreshToken(MeetUser user) {
    	
    	// 아직 만료되지 않고, 폐기되지 않은 기존 토큰이 있는지 먼저 검색 
    	Optional<RefreshToken> existingTokenOpt = refreshTokenRepository
    	    .findFirstByUserAndRevokedIsFalseAndExpiresAtAfter(user, LocalDateTime.now());
    	
    	if (existingTokenOpt.isPresent()) {
    	    // 2. 기존에 유효한 토큰이 있으면 기존 토큰을 그대로 반환해 재사용 (중복 데이터, 쓸데없는 토큰 저장 방지)    
    	    return existingTokenOpt.get();
    	}

    	// 3. 유효한 토큰이 없을때 (만료, 폐기, 처음 로그인)
    	String token = UUID.randomUUID().toString(); 		// 새 랜덤 토큰(UUID) 발급
    	LocalDateTime issuedAt = LocalDateTime.now(); 		// 발급 시각
    	LocalDateTime expiresAt = issuedAt.plusDays(7); 	// 7일 뒤 만료일 구하기

    	// 4. RefreshToken 엔티티 생성. 사용자를 연결
    	RefreshToken refreshToken = RefreshToken.builder()
    	        .refreshToken(token)     // 실제 토큰 문자열
    	        .issuedAt(issuedAt)     // 발급 시각
    	        .expiresAt(expiresAt)   // 만료 시각
    	        .revoked(false)         // 아직 폐기 아님
    	        .user(user)             // 토큰 소유자 연결
    	        .build();

    	// 5. DB에 저장 후 생성된 토큰 엔티티 반환
    	return refreshTokenRepository.save(refreshToken);
    }

    // 2. 토큰 문자열로 DB에서 리프레시 토큰 조회
    @Override
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByRefreshToken(token);
    }

    // 3. 토큰 유효성 검증: 존재 여부, 폐기 상태, 만료 여부 확인
    @Override
    public RefreshToken verifyTokenValidity(String token) {
        // DB에서 토큰 조회, 없으면 예외 발생
        RefreshToken refreshToken = findByToken(token)
                .orElseThrow(() -> new RuntimeException("리프레시 토큰이 존재하지 않습니다."));

        // 폐기된 토큰인지 체크 후 예외 발생
        if (refreshToken.isRevoked()) {
            throw new RefreshTokenException("리프레시 토큰이 폐기되었습니다.");
        }

        // 만료된 토큰인지 체크 후 예외 발생
        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RefreshTokenException("리프레시 토큰이 만료되었습니다.");
        }

        // 모든 검사 통과하면 해당 토큰 리턴
        return refreshToken;
    }

    // 4. 토큰 폐기 처리: 토큰을 찾아 revoked=true 로 표시하고 DB 업데이트
    @Override
    public void revokeToken(String token) {
        findByToken(token).ifPresent(rt -> {
            rt.setRevoked(true);                // 폐기 상태로 상태 변경
            refreshTokenRepository.save(rt);   // 저장소에 변경 상태 반영
            log.info("리프레시 토큰이 폐기되었습니다.");
        });
    }

    // 5. 만료된 토큰 정리: 현재 시각 이전에 만료된 모든 토큰을 DB에서 삭제
    @Override
    @Transactional
    public void deleteExpiredTokens() {
        refreshTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        log.info("만료된 리프레시 토큰 정리 완료");
    }
    
    // 리프레시 토큰 생성 후 DTO로 변환 반환 (외부 API 로 넘길때 해당 메서드 사용)
    public RefreshTokenDto createRefreshTokenDto(MeetUser user) {
        RefreshToken token = this.createRefreshToken(user);
        return RefreshTokenDto.fromEntity(token);
    }

    // 토큰 문자열로 조회 후 DTO로 변환 반환 (외부 API 로 넘길때 해당 메서드 사용)
    public RefreshTokenDto getValidRefreshTokenDto(String token) {
        RefreshToken refreshToken = this.verifyTokenValidity(token);
        return RefreshTokenDto.fromEntity(refreshToken);
    }
  
    // User 조회 후 Refresh 토큰 삭제
    @Override
    @Transactional
    public void deleteByEmail(String email) {
        refreshTokenRepository.deleteByUserEmail(email); 
    }
    
    
}