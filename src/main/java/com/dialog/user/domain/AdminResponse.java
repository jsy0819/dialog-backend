package com.dialog.user.domain;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
public class AdminResponse {

    private final Long id;          // PK (수정/삭제용)
    private final String name;      // 이름
    private final String email;     // 이메일
    private final String role;      // 권한 (ADMIN, USER)
    private final boolean active;   // 계정 활성 상태
    private final LocalDateTime regDate; // 가입일
    
    // 추가 했음.
    private final String job;
    private final String position;
    
    public static AdminResponse from(MeetUser user) {
        return AdminResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole() != null ? user.getRole().name() : "USER")
                .active(user.isActive()) // Entity에 추가한 메서드 사용
                .regDate(user.getCreatedAt()) // Entity의 생성일 필드 사용
                .job(user.getJob() != null ? user.getJob().name() : null)
                .position(user.getPosition() != null ? user.getPosition().name() : null)
                .build();
    }
}