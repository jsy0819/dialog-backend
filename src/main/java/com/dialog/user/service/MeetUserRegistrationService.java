package com.dialog.user.service;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.dialog.security.oauth2.SocialUserInfo;
import com.dialog.user.domain.MeetUser;
import com.dialog.user.repository.MeetUserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MeetUserRegistrationService {

    private final MeetUserRepository meetUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public MeetUser saveOrUpdateSocialMember(SocialUserInfo socialUserInfo, String provider) {
    	
    	try {
    	    // 1. 소셜 로그인 사용자의 고유 식별자 생성 (예: google_nureong)
    	    String socialId = provider + "_" + socialUserInfo.getId();

    	    // 2. DB에서 기존 사용자 검색
    	    Optional<MeetUser> existingUserOpt = this.meetUserRepository.findBySnsId(socialId);

    	    // 3. 기존 사용자라면 정보 업데이트 후 저장
    	    if (existingUserOpt.isPresent()) {
    	        MeetUser existingUser = existingUserOpt.get();
    	        existingUser.updateSocialInfo(
    	            socialUserInfo.getName(),
    	            socialUserInfo.getProfileImageUrl(),
    	            socialId,
    	            provider
    	        );
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

    	        return meetUserRepository.save(newUser);
    	    }
    	} catch (Exception e) {
    	    throw new RuntimeException("소셜 사용자 저장 중 오류가 발생했습니다.", e);
    	}
    }
    // 이메일이 없는 경우 임시 이메일 생성 메서드 (사용자 구분용)
    private String generateTempEmail(String name) {
        return name.replaceAll("\\s+", "") + "_" + UUID.randomUUID().toString().substring(0, 8) + "@social.user.temp";
    }

    // 이메일 기반 고유 사용자명 생성 및 DB 중복 검사
    private String generateUniqueEmail(String email) {
        if(email == null) {
            return "social_user_" + UUID.randomUUID().toString().substring(0, 8);
        }
        String baseEmail = email.split("@")[0];
        String username = baseEmail;
        int cnt = 1;
        //  중복될 경우 숫자 뒤에 붙여서 고유명 생성
        while (meetUserRepository.existsByEmail(username)) {
        	username = baseEmail + "_" + cnt++;
        }
        return username;
    }
    
    
}


