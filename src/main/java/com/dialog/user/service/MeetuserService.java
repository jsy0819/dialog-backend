package com.dialog.user.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dialog.email.service.EmailService;
import com.dialog.exception.InactiveUserException;
import com.dialog.exception.InvalidPasswordException;
import com.dialog.exception.OAuthUserNotFoundException;
import com.dialog.exception.TermsNotAcceptedException;
import com.dialog.exception.UserAlreadyExistsException;
import com.dialog.exception.UserNotFoundException;
import com.dialog.exception.UserRoleAccessDeniedException;
import com.dialog.security.oauth2.SocialUserInfo;
import com.dialog.security.oauth2.SocialUserInfoFactory;
import com.dialog.user.domain.MeetUser;
import com.dialog.user.domain.MeetUserDto;
import com.dialog.user.domain.Role;
import com.dialog.user.domain.UserSettingsUpdateDto;
import com.dialog.user.repository.MeetUserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor 
public class MeetuserService {

    private final MeetUserRepository meetUserRepository; 
    private final PasswordEncoder passwordEncoder;       
    private final EmailService emailService;

    @Value("${app.reset-password.url}")
    private String resetPasswordUrl;
    
//      회원가입 처리 메서드
//      가입정보 DTO (이메일, 비번 등)
//      약관 동의 체크 -> 이메일 중복 체크 -> 비번 암호화 및 DB저장
//      예외케이스 발생시: IllegalArgumentException, IllegalStateException 던짐 (글로벌 핸들러에서 catch됨)
     
    public void signup(MeetUserDto dto) {
        // 1. 약관 동의 필수 검사
        if (dto.getTerms() == null || !dto.getTerms()) {
            throw new TermsNotAcceptedException("약관에 동의해야 가입할 수 있습니다.");
        }
        // 2. 이메일 중복검사	
        if (meetUserRepository.existsByEmail(dto.getEmail())) {
            throw new UserAlreadyExistsException("이미 가입된 이메일입니다.");
        }
        // 3. DTO → Entity 변환, 비밀번호 암호화 후 저장
        MeetUser user = MeetUser.builder()
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .name(dto.getName())
                .socialType(dto.getSocialType())
                .profileImgUrl(dto.getProfileImgUrl())
                .snsId(dto.getSnsId())
                .role(Role.USER)
                .build();
        meetUserRepository.save(user); // DB에 신규 회원 저장
    }

    
//      현재 로그인한 유저 정보 조회 
//      인증 안됐으면 예외 발생
//      소셜(OAuth2) 로그인과 일반 로그인 분기
//         - OAuth2User면 provider, snsId로 회원 조회(DB)
//         - UserDetails면 username(email)로 회원 조회(DB)
//      최종적으로 MeetUserDto로 변환해 반환
    
    // 현재 로그인한 유저 정보 조회
    public MeetUserDto getCurrentUser(Authentication authentication) {
        // 1. 공용 메서드를 호출해 사용자 엔티티를 가져옴
        MeetUser user = getAuthenticatedUser(authentication);
        
        // 2. 엔티티를 DTO로 변환해 반환
        return MeetUserDto.fromEntity(user);
    }
    
//      로그인 검증
//      이메일 DB조회 -> 비밀번호 비교 (암호화 검증) -> 오류시 예외 반환, 성공시 유저 엔티티 반환     
    public MeetUser login(String email, String rawPassword) {
        MeetUser user = meetUserRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("존재하지 않는 사용자 입니다."));
        
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new InvalidPasswordException("비밀번호가 올바르지 않습니다.");
        }
        
        if (!user.isActive()) {
            throw new InactiveUserException("비활성화된 사용자입니다. 문의해 주세요.");
        }
        
        return user;
    }
    
    // 설정 페이지에서 직무/직급 업데이트
    @Transactional
    public void updateUserSettings(Authentication authentication, UserSettingsUpdateDto dto) {
        
        // 1. 현재 인증된 사용자 엔티티를 조회
        MeetUser user = getAuthenticatedUser(authentication);

        // 2. MeetUser 엔티티 내부의 업데이트 메서드 호출
        user.updateSettings(dto.getJob(), dto.getPosition());
    }
    
    // 인증된 사용자 엔티티 조회
    private MeetUser getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UserRoleAccessDeniedException("로그인 세션 없음 또는 권한이 없습니다.");
        }
        Object principal = authentication.getPrincipal();
        MeetUser user;

        if (principal instanceof OAuth2User) {
            OAuth2AuthenticationToken authToken = (OAuth2AuthenticationToken) authentication;
            String provider = authToken.getAuthorizedClientRegistrationId();
            OAuth2User oAuth2User = (OAuth2User) principal;
            SocialUserInfo info = SocialUserInfoFactory.getSocialUserInfo(provider, oAuth2User.getAttributes());
            String snsId = provider + "_" + info.getId();
            user = meetUserRepository.findBySnsId(snsId)
                .orElseThrow(() -> new OAuthUserNotFoundException("회원 정보를 찾을 수 없습니다."));
        } else if (principal instanceof UserDetails) {
            String username = ((UserDetails) principal).getUsername();
            user = meetUserRepository.findByEmail(username)
                .orElseThrow(() -> new UserNotFoundException("회원 정보를 찾을 수 없습니다."));
        } else {
            throw new UserRoleAccessDeniedException("알 수 없는 인증 방식입니다.");
        }
        return user;
    }
    
    // 비밀번호 초기화 이메일 발송 메서드
    public void sendResetPasswordEmail(String email) {
        MeetUser user = meetUserRepository.findByEmail(email)
            .orElseThrow(() -> new UserNotFoundException("존재하지 않는 이메일 입니다."));

        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);

        user.setResetPasswordToken(token, expiresAt);
        meetUserRepository.save(user);

        String resetUrl = resetPasswordUrl + "?token=" + token;

        emailService.sendPasswordResetEmail(user.getEmail(), resetUrl);
    }

    // 비밀번호 초기화 메서드
    public void resetPassword(String token, String newPassword) {
        MeetUser user = meetUserRepository.findByResetPasswordToken(token)
            .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 토큰입니다."));

        // 토큰 만료 확인
        if (!user.isResetTokenValid()) {
            throw new IllegalArgumentException("토큰이 만료되었습니다.");
        }

        // 비밀번호 암호화 및 저장 
        String encodedPassword = passwordEncoder.encode(newPassword);
        user.setPassword(encodedPassword);

        // 토큰 무효화
        user.clearResetPasswordToken();

        meetUserRepository.save(user);
    }
    
}
