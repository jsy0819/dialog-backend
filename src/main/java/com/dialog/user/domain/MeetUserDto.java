package com.dialog.user.domain;

import com.sun.istack.NotNull;

import jakarta.persistence.Column;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * MeetUser 엔티티 기반 User DTO 클래스 (외부 노출 모델)
 */
@Getter
@Setter
@NoArgsConstructor // 추가
public class MeetUserDto {

    @NotBlank
    @Email
    @Size(max = 100)
    private String email; // 이메일 주소

    @NotBlank
    @Size(min = 8, max = 200)
    private String password;
    
    @NotNull
    private Boolean terms;

    @NotBlank
    @Size(max = 50)
    private String name;

    @Size(max = 200)
    private String department;

    @Size(max = 50)
    private String position;

    @Size(max = 50)
    private String socialType;

    @Size(max = 50)
    private String profileImgUrl;

    @Size(max = 100)
    private String snsId;

    @Builder
    public MeetUserDto(String email, String password, Boolean terms, String name, String department, String position,
                       String socialType, String profileImgUrl, String snsId) {
        this.email = email;
        this.password = password;
        this.terms = terms;
        this.name = name;
        this.department = department;
        this.position = position;
        this.socialType = socialType;
        this.profileImgUrl = profileImgUrl;
        this.snsId = snsId;
    }
    
    // Entity -> Dto로 변환하는 생성자/정적 메소드
    public static MeetUserDto fromEntity(MeetUser user) {
        MeetUserDto dto = new MeetUserDto();
        dto.setEmail(user.getEmail());
        dto.setName(user.getName());
        return dto;
    }


}