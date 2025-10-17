package com.dialog.user.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import com.dialog.security.oauth2.SocialUserInfo;
import com.dialog.security.oauth2.SocialUserInfoFactory;
import com.dialog.user.domain.MeetUser;
import com.dialog.user.domain.MeetUserDto;
import com.dialog.user.repository.MeetUserRepository;

import lombok.RequiredArgsConstructor;

@Service // 서비스 계층(DB 접근·비즈니스 로직 처리 담당)
@RequiredArgsConstructor // 생성자 주입
public class MeetuserService {

    private final MeetUserRepository meetUserRepository; // DB 접근 리포지토리
    private final PasswordEncoder passwordEncoder;       // 비밀번호 암호화/검증

    
//      회원가입 처리 메서드
//      @param dto 가입정보 DTO (이메일, 비번 등)
//      흐름: 1) 약관 동의 체크 2) 이메일 중복 체크 3) 비번 암호화 및 DB저장
//      예외케이스 발생시: IllegalArgumentException, IllegalStateException 던짐 (글로벌 핸들러에서 catch됨)
//      @throws Exception 가입에 실패 시 예외 발생
     
    public void signup(MeetUserDto dto) {
        // 1. 약관 동의 필수 검사
        if (dto.getTerms() == null || !dto.getTerms()) {
            throw new IllegalArgumentException("약관에 동의해야 가입할 수 있습니다.");
        }
        // 2. 이메일 중복검사
        if (meetUserRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalStateException("이미 가입된 이메일입니다.");
        }
        // 3. DTO → Entity 변환, 비밀번호 암호화 후 저장
        MeetUser user = MeetUser.builder()
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .name(dto.getName())
                .department(dto.getDepartment())
                .position(dto.getPosition())
                .socialType(dto.getSocialType())
                .profileImgUrl(dto.getProfileImgUrl())
                .snsId(dto.getSnsId())
                .build();
        meetUserRepository.save(user); // DB에 신규 회원 저장
    }

    
//      현재 로그인한 유저 정보 조회 (SecurityContext 기반)
//      @param authentication Spring Security 인증객체
//      흐름:
//       1) 인증 안됐으면 예외 발생
//      2) 소셜(OAuth2) 로그인과 일반 로그인 분기
//         - OAuth2User면 provider, snsId로 회원 조회(DB)
//         - UserDetails면 username(email)로 회원 조회(DB)
//      3) 최종적으로 MeetUserDto로 변환해 반환
    
    public MeetUserDto getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("로그인 세션 없음");
        }
        Object principal = authentication.getPrincipal();
        MeetUser user;
        // 소셜 로그인 (OAuth2User) 처리
        if (principal instanceof OAuth2User) {
            OAuth2AuthenticationToken authToken = (OAuth2AuthenticationToken) authentication;
            String provider = authToken.getAuthorizedClientRegistrationId();
            OAuth2User oAuth2User = (OAuth2User) principal;
            SocialUserInfo info = SocialUserInfoFactory.getSocialUserInfo(provider, oAuth2User.getAttributes());
            String snsId = provider + "_" + info.getId();
            user = meetUserRepository.findBySnsId(snsId)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));
        // 일반 로그인 (UserDetails) 처리
        } else if (principal instanceof UserDetails) {
            String username = ((UserDetails) principal).getUsername();
            user = meetUserRepository.findByEmail(username)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));
        } else {
            throw new IllegalArgumentException("알 수 없는 인증 방식입니다.");
        }
        // Entity -> DTO 변환해 반환
        return MeetUserDto.fromEntity(user);
    }

    
//      로그인 검증
//      @param email 로그인 아이디(이메일)
//      @param rawPassword 평문 비밀번호
//      흐름:
//      1) 이메일 DB조회
//      2) 비밀번호 비교 (암호화 검증)
//      3) 오류시 예외 반환, 성공시 유저 엔티티 반환
     
    public MeetUser login(String email, String rawPassword) {
        MeetUser user = meetUserRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 아이디입니다."));
        // 비밀번호 불일치 시 예외 발생
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new IllegalStateException("비밀번호가 올바르지 않습니다.");
        }
        return user; // 인증 성공 시 회원 정보 반환
    }
}
