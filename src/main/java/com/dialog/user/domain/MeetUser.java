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


 // 사용자 계정 정보를 담는 엔티티 클래스.
 // - 데이터베이스의 'user' 테이블과 직접 매핑되며, 애플리케이션의 핵심 데이터 모델입니다.
 // - @Getter: Lombok을 통해 모든 필드의 Getter 메서드를 자동 생성합니다.
 
@Entity
@Table(name = "user")
@Getter
public class MeetUser {

    // --- 필드 순서 재정렬 ---
    // 1. 기본키 (PK)
    // 2. 계정 정보 (이메일, 비밀번호)
    // 3. 사용자 개인 정보 (이름, 프로필 사진, 직무, 직급)
    // 4. 소셜 로그인 정보
    // 5. 메타 정보 (생성일)

    
    //  1. 기본키(Primary Key)
    //  - @Id: 이 필드가 테이블의 기본키임을 나타냅니다.
    // - @GeneratedValue: 기본키 값을 데이터베이스가 자동으로 생성하도록 설정합니다. (Auto Increment)
     
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

    
     //  2-1. 이메일 (로그인 ID)
     //  - @Column: 테이블의 컬럼에 매핑되는 필드임을 나타냅니다.
     //  - nullable = false: 이 컬럼은 null 값을 허용하지 않습니다. (필수값)
     //  - unique = true: 이 컬럼의 값은 테이블 내에서 유일해야 합니다.
     
	@Column(nullable = false, length = 100, unique = true)
	private String email;

    
    // 2-2. 비밀번호
    //  - 암호화된 형태로 저장되므로 길이를 넉넉하게 255자로 설정합니다.
     
	@Column(nullable = false, length = 255)
	private String password;

    
    //  3-1. 사용자 실명
     
	@Column(nullable = false, length = 100)
	private String name;

    
    //  3-2. 프로필 이미지 URL
    // - 사용자가 프로필 사진을 설정했을 경우, 해당 이미지의 URL이 저장됩니다.
     
	@Column(length = 200)
	private String profileImgUrl;

    
    // 3-3. 직무 (Job)
    // - @Enumerated(EnumType.STRING): Enum 타입을 DB에 저장할 때, 순서(ORDINAL)가 아닌 이름(STRING)으로 저장합니다.
    // (이유: 중간에 Enum 순서가 바뀌어도 DB 데이터가 깨지지 않아 훨씬 안전합니다.)
     
	@Enumerated(EnumType.STRING)
	@Column(length = 50, nullable = false)
	private Job job = Job.NONE; // 기본값으로 '정해지지 않음'을 설정

    
    // 3-4. 직급 (position)
    // - 직무(Job) 필드와 동일한 이유로 EnumType.STRING을 사용합니다.
     
	@Enumerated(EnumType.STRING)
	@Column(length = 50, nullable = false)
	private Position position = Position.NONE; // 기본값으로 '정해지지 않음'을 설정

    
    // 4-1. 소셜 로그인 타입 (e.g., "google", "kakao")
     
	@Column(length = 50)
	private String socialType;

    
     // 4-2. 소셜 로그인 고유 ID
     // - 각 소셜 플랫폼에서 제공하는 사용자의 고유 식별자입니다.
     
	@Column(length = 100)
	private String snsId;

    
    //  5. 계정 생성일
    //  - @CreationTimestamp: 엔티티가 처음 저장될 때의 시간이 자동으로 기록됩니다.
    //  - updatable = false: 한 번 생성된 후에는 값이 수정되지 않습니다.
     
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
    
    
	
    
    //  JPA를 위한 기본 생성자.
    //  - JPA가 프록시 객체를 생성할 때 이 생성자를 사용합니다.
    //  - `protected`로 선언하여 외부에서 `new MeetUser()`와 같이 무분별하게 객체를 생성하는 것을 방지합니다.
     
	protected MeetUser() {
	}

	
	//  @Builder: 빌더 패턴으로 객체를 생성할 수 있게 합니다.
	//   - 생성자의 파라미터 순서와 상관없이 명확하게 값을 할당할 수 있어 안전합니다.
	//   - 일반 회원가입 시에는 email, password, name만 사용됩니다.
	 
	@Builder
	public MeetUser(String email, String password, String name, String socialType, String snsId, String profileImgUrl) {
		this.email = email;
		this.password = password;
		this.name = name;
		this.socialType = socialType;
		this.snsId = snsId;
		this.profileImgUrl = profileImgUrl;
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

	
	 // '설정' 페이지에서 직무/직급 업데이트를 위한 메서드.
	 //  MeetuserService에서 호출됩니다.
	 
	public void updateSettings(Job job, Position position) {
		this.job = job;
		this.position = position;
	}

}
