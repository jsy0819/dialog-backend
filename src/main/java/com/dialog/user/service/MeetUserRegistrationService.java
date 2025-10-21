package com.dialog.user.service;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.dialog.security.jwt.JwtTokenProvider;
import com.dialog.security.oauth2.SocialUserInfo;
import com.dialog.user.domain.MeetUser;
import com.dialog.user.repository.MeetUserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MeetUserRegistrationService {

    private final MeetUserRepository meetUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public MeetUser saveOrUpdateSocialMember(SocialUserInfo socialUserInfo, String provider) {
    	
    	try {
    	    // 1. 소셜 로그인 사용자의 고유 식별자 생성 (예: google_nureong)
    		log.info("소셜 사용자 정보 - 이름: {}, 이메일: {}, Provider: {}", socialUserInfo.getName(), socialUserInfo.getEmail(), provider);
    	    String socialId = provider + "_" + socialUserInfo.getId();

    	    // 2. DB에서 기존 사용자 검색
    	    Optional<MeetUser> existingUserOpt = this.meetUserRepository.findBySnsId(socialId);

    	    // 3. 기존 사용자라면 정보 업데이트 후 저장
    	    if (existingUserOpt.isPresent()) {
    	        MeetUser existingUser = existingUserOpt.get();
    	        log.info("기존 회원 발견 - 기존 이름: {}, 기존 이메일: {}", existingUser.getName(), existingUser.getEmail());

    	        existingUser.updateSocialInfo(
    	            socialUserInfo.getName(),
    	            socialUserInfo.getProfileImageUrl(),
    	            socialId,
    	            provider
    	        );
    	        log.info("기존 회원 발견 - 기존 이름: {}, 기존 이메일: {}", existingUser.getName(), existingUser.getEmail());

    	        return meetUserRepository.save(existingUser);
    	    }
    	    // 4. 신규 사용자라면 새로 사용자 생성 후 저장
    	    else {
    	        MeetUser newUser = new MeetUser();
    	        // 4-1. 이메일 기반 고유 사용자명 생성(중복 체크 포함)
    	        newUser.setEmail(generateUniqueEmail(socialUserInfo.getEmail()));
    	        newUser.setName(socialUserInfo.getName());
    	        // 4-2. 임시 비밀번호를 BCrypt 인코딩하여 저장
    	        newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
    	        // 4-3. 소셜 ID, SNS 종류, 프로필 이미지 URL 설정
    	        newUser.setSnsId(socialId);
    	        newUser.setSocialType(provider);
    	        newUser.setProfileImgUrl(socialUserInfo.getProfileImageUrl());
    	        
                log.info("신규 사용자 생성 - 이름: {}, 이메일: {}", newUser.getName(), newUser.getEmail());
    	        return meetUserRepository.save(newUser);
    	    }
    	} catch (Exception e) {
    		log.error("소셜 사용자 저장 중 오류 발생", e);
    	    throw new RuntimeException("소셜 사용자 저장 중 오류가 발생했습니다.", e);
    	}
    }

    // 이메일 기반 고유 사용자명 생성 및 DB 중복 검사
    private String generateUniqueEmail(String email) {
    	
    	// 1. null 또는 빈 문자열 ("")일 때 임시 이메일 생성 
    	if (email == null || email.isEmpty()) { 
            return "temp_social_" + UUID.randomUUID().toString().substring(0, 8) + "@dialog.com";
        }

        // 2. 이메일 파싱 (정상적인 이메일일 경우)
        String[] parts = email.split("@");
        if (parts.length != 2) {
             // @가 없는 비정상적인 이메일인 경우도 임시 이메일로 처리
             return "invalid_social_" + UUID.randomUUID().toString().substring(0, 8) + "@dialog.com";
        }
        
        String baseEmail = parts[0];
        String domain = parts[1]; // 도메인 파트 분리
        String username = baseEmail;
        int cnt = 1;

        // 3. 중복 검사 시 완전한 이메일 주소 사용 
        while (meetUserRepository.existsByEmail(username + "@" + domain)) { 
            username = baseEmail + "_" + cnt++;
        }
        
        // 4. 완전한 이메일 주소를 반환 (오류 2 수정)
        return username + "@" + domain;
    }
    
}


