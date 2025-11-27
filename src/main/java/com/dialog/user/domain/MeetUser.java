package com.dialog.user.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import com.dialog.token.domain.UserSocialToken;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;


 
@Entity
@Table(name = "user")
@Getter
@Setter
public class MeetUser {


     
   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private Long id;

    
    // 이메일 (로그인 ID)
   @Column(nullable = false, length = 100, unique = true)
   private String email;

    
    //  비밀번호
   @Column(nullable = false, length = 255)
   private String password;

    
    //  사용자 실명
   @Column(nullable = false, length = 100)
   private String name;

    
    // 프로필 이미지 URL
   @Column(length = 200)
   private String profileImgUrl;

    
    // 직무 
   @Enumerated(EnumType.STRING)
   @Column(length = 50, nullable = false)
   private Job job = Job.NONE; // 기본값으로 '정해지지 않음'을 설정

    
    // 직급
   @Enumerated(EnumType.STRING)
   @Column(length = 50, nullable = false)
   private Position position = Position.NONE; // 기본값으로 '정해지지 않음'을 설정

    
    // 소셜 로그인 타입 (e.g., "google", "kakao")    
   @Column(length = 50)
   private String socialType;

   @Column(nullable = false)
   private boolean active = true;
   
    // 소셜 로그인 고유 ID
   @Column(length = 100)
   private String snsId;

    
   //계정 생성일
   @CreationTimestamp
   @Column(name = "created_at", nullable = false, updatable = false)
   private LocalDateTime createdAt;

   	// 소셜 토큰
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserSocialToken> socialTokens = new ArrayList<>();
    
    // 소셜토큰 추가
    public void addSocialToken(UserSocialToken token) {
        socialTokens.add(token);
        token.setUser(this);
    }
    
    // 유저 권한 추가
    @Enumerated(EnumType.STRING)
    private Role role;
    
    // 비밀번호 재설정 추가
    @Column(name = "reset_password_token", length = 255)
    private String resetPasswordToken;

    // 토큰 검사 추가
    @Column(name = "reset_token_expires_at")
    private LocalDateTime resetTokenExpiresAt;


    //  JPA를 위한 기본 생성자.
   protected MeetUser() {
   }
   
   @Builder
   public MeetUser(String email, String password, String name, String socialType, String snsId, String profileImgUrl, Role role) {
      this.email = email;
      this.password = password;
      this.name = name;
      this.socialType = socialType;
      this.snsId = snsId;
      this.profileImgUrl = profileImgUrl;
      // role 값이 null 일 때 기본값을 USER로 설정
      this.role = role != null ? role : Role.USER; 
      this.active = true; // 빌더 사용 시에도 기본값 true 설정
   }
   
   public void deactivate() {
       this.active = false;
   }   
   
    // 소셜 로그인 시, 기존 소셜 토큰 업데이트 메서드    
    public void updateSocialToken(String provider, String accessToken, String refreshToken, LocalDateTime expiresAt) {
        for (UserSocialToken token : socialTokens) {
            if (token.getProvider().equals(provider)) {
                token.setAccessToken(accessToken);
                token.setRefreshToken(refreshToken);
                token.setExpiresAt(expiresAt);
                return;
            }
        }
        // 없으면 새 토큰 추가
        UserSocialToken newToken = new UserSocialToken();
        newToken.setProvider(provider);
        newToken.setAccessToken(accessToken);
        newToken.setRefreshToken(refreshToken);
        newToken.setExpiresAt(expiresAt);
        addSocialToken(newToken);
    }
    
    // 소셜 로그인 시, 기존 회원이 재로그인하면 프로필 정보(이름, 사진)를 업데이트하는 메서드.    
   public void updateSocialInfo(String name, String profileImgUrl) {
      this.name = name;
      this.profileImgUrl = profileImgUrl;
   }

   
    // 설정 페이지에서 직무/직급 업데이트를 위한 메서드.    
   public void updateSettings(Job job, Position position) {
      this.job = job;
      this.position = position;
   }
   
   
   public void setResetPasswordToken(String token, LocalDateTime expiresAt) {
	    this.resetPasswordToken = token;
	    this.resetTokenExpiresAt = expiresAt;
	}

	public void clearResetPasswordToken() {
	    this.resetPasswordToken = null;
	    this.resetTokenExpiresAt = null;
	}

	public boolean isResetTokenValid() {
	    return resetPasswordToken != null && resetTokenExpiresAt != null && resetTokenExpiresAt.isAfter(LocalDateTime.now());
	}

}
