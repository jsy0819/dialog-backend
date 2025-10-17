package com.dialog.user.domain;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

//권한(role) 컬럼이 없어서 권한 확장이 필요할 경우 엔티티에 role 컬럼이나 별도 Role 테이블 연동이 필요하다.

//계정 상태(잠금, 비활성화, 만료) 컬럼이 없는데, 보안 강화 필요 시 추가 권장.

//패스워드 길이 200자 지정은 넉넉하지만 비밀번호 암호화 정책에 따라 충분히 커버하는지 확인 필요.

//SNS ID 필드 unique 설정이 필요하면 인덱스 및 제약 조건을 DB쪽에서 추가 고려.

@Entity
@Getter
@Setter
@Table(name = "meet_user")
public class MeetUser {

    // 1. 기본키 ID, 자동 증가 전략 사용
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 유저에 대한 고유 id값

    // 2. 로그인 시 사용되는 사용자명. 반드시 유니크하며 널 불가, 길이는 최대 100자
    @Column(nullable = false, length = 100, unique = true)
    private String email;

    // 3. 로그인 비밀번호, 길이는 최대 200자, 널 불가
    @Column(nullable = false, length = 200)
    private String password;

    // 4. 사용자 이름 (실명)
    @Column(nullable = false, length = 50)
    private String name;
    
    // 5. 부서명, 선택적 필드
    @Column(length = 200)
    private String department;

    // 6. 직급, 선택적 필드
    @Column(length = 50)
    private String position;

    // 7. 소셜 로그인 플랫폼 구분용(google, kakao 등)
    @Column(length = 50)
    private String socialType;

    // 8. 사용자 프로필 이미지 URL
    @Column(length = 200)
    private String profileImgUrl;

    // 9. 소셜 로그인 시 사용하는 고유 SNS 아이디
    @Column(length = 100)
    private String snsId;

    // 10. 기본 생성자 (JPA 필요)
    public MeetUser() {}

    // 11. 모든 필드를 초기화하는 생성자 (테스트나 수동 객체 생성용)
    @Builder
    public MeetUser(Long id, String email, String password, String name, String department, String position,
                    String socialType, String profileImgUrl, String snsId) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.name = name;
        this.department = department;
        this.position = position;
        this.socialType = socialType;
        this.profileImgUrl = profileImgUrl;
        this.snsId = snsId;
    }

    // 12. 소셜 로그인 시 기존 사용자가 재로그인하면 이름과 프로필 이미지 URL만 업데이트 할 수 있는 메서드
    public void updateSocialInfo(String name, String profileImgUrl, String snsId, String socialType) {
       this.name = name;
       this.profileImgUrl = profileImgUrl;
       this.snsId = snsId;
       this.socialType = socialType;
    }
    
    // 계정 잠금, 비활성화 필요할시 사용.
//    @Column(nullable = false)
//    private boolean accountExpired = false;
//
//    @Column(nullable = false)
//    private boolean accountLocked = false;
//
//    @Column(nullable = false)
//    private boolean credentialsExpired = false;
//
//    @Column(nullable = false)
//    private boolean disabled = false;
    
}
