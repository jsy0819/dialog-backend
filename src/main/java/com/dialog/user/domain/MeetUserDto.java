package com.dialog.user.domain;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
public class MeetUserDto {

	// 회원가입(Request) 시 사용되는 필드
	@NotBlank
	@Email
	@Size(max = 100)
	private String email;

	@NotBlank
	@Size(min = 8, max = 200)
	private String password;

	@NotNull
	private Boolean terms;

	@NotBlank
	@Size(max = 100)
	private String name;

	// 정보 조회 시 사용되는 필드 
	private String profileImgUrl;
	private String job; 
	private String position; 

	// 소셜 로그인 관련 필드
	private String socialType;
	private String snsId;
	
	// 권한 추가
	private String role; // 추가

	@Builder
    public MeetUserDto(String email, String password, Boolean terms, String name,
            String profileImgUrl, String job, String position, String socialType, String snsId, String role) {
	this.email = email;
	this.password = password;
	this.terms = terms;
	this.name = name;
	this.profileImgUrl = profileImgUrl;
	this.job = job;
	this.position = position;
	this.socialType = socialType;
	this.snsId = snsId;
	this.role = role;
	}


    // Service 계층에서 DB로부터 조회한 Entity를 클라이언트에 보낼 DTO로 변환할 때 사용합니다.
    public static MeetUserDto fromEntity(MeetUser user) {
        return MeetUserDto.builder()
                .email(user.getEmail())
                .name(user.getName())
                .profileImgUrl(user.getProfileImgUrl())
                .job(user.getJob().name())
                .position(user.getPosition().name())
                .socialType(user.getSocialType())
                .snsId(user.getSnsId())
                .role(user.getRole() != null ? user.getRole().name() : null) // 권한도 함께 변환
                .build();
    }

}
