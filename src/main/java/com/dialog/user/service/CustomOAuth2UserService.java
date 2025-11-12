package com.dialog.user.service;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dialog.exception.SocialUserSaveException;
import com.dialog.security.oauth2.CustomOAuth2User;
import com.dialog.security.oauth2.SocialUserInfo;
import com.dialog.security.oauth2.SocialUserInfoFactory;
import com.dialog.user.domain.MeetUser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final SocialRegistrationService registrationService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 1. 기본 DefaultOAuth2UserService를 사용해 외부 OAuth 공급자로부터 사용자 정보를 조회
        DefaultOAuth2UserService defaultService = new DefaultOAuth2UserService();
        OAuth2User oauth2User = defaultService.loadUser(userRequest);

        // 2. 현재 로그인 진행중인 OAuth 공급자 식별 (google, kakao, naver 등)
        String registId = userRequest.getClientRegistration().getRegistrationId();
        // 3. 자동 로그인에 사용할 사용자의 ID 속성 이름 추출
        String userNameAttrName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        // 4. 공급자 별로 다른 사용자 정보를 추상화하여 통합 처리
        SocialUserInfo socialUserInfo = SocialUserInfoFactory.getSocialUserInfo(
                registId, oauth2User.getAttributes()
        );
        // 5. 내부 DB에 사용자 정보 저장 또는 업데이트 (최종 로그인 시각 반영 등)

        MeetUser user;
        try {
            user = registrationService.saveOrUpdateSocialMember(socialUserInfo, registId);
        } catch (SocialUserSaveException e) {
            log.error("소셜 사용자 정보 저장 실패", e);
            // 커스텀 예외를 명확히 처리하여 재던짐
            throw new OAuth2AuthenticationException("소셜 사용자 정보를 저장하는 중 오류가 발생했습니다.");
        } catch (Exception e) {
            log.error("소셜 사용자 정보 처리 중 예상치 못한 오류", e);
            // 예상치 못한 예외는 별도로 처리
            throw new OAuth2AuthenticationException("소셜 사용자 정보 처리 중 오류가 발생했습니다.");
        }
        
        if (user == null) {
            throw new OAuth2AuthenticationException("회원 정보를 찾을 수 없습니다.");
        }

        // 6. 권한 설정 (여기서는 임시로 USER_ROLE 권한 부여)
        String roleName = user.getRole() != null ? user.getRole().name() : "USER";
        Set<SimpleGrantedAuthority> authorities = Set.of(
            new SimpleGrantedAuthority("ROLE_" + roleName)
        );

        // 7. CustomOAuth2User 객체 생성해 리턴
        return new CustomOAuth2User(
                authorities,
                oauth2User.getAttributes(),
                userNameAttrName,
                user
            );
    }
}
