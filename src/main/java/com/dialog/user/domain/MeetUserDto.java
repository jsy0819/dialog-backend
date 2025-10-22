package com.dialog.user.domain;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 사용자 데이터 전송 객체 (Data Transfer Object).
 * - 클라이언트와 서버 간 데이터 교환에 사용되는 통합 클래스입니다.
 * - Entity의 모든 필드를 노출하지 않고, 필요한 데이터만 선택하여 전달하는 역할을 합니다.
 */
@Getter
@Setter
@NoArgsConstructor
public class MeetUserDto {

	// --- 회원가입(Request) 시 사용되는 필드 ---
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

	// --- 정보 조회(Response) 시 사용되는 필드 ---
	private String profileImgUrl;
	private String job; // Enum 타입인 Job을 String으로 변환하여 전달
	private String position; // Enum 타입인 position를 String으로 변환하여 전달

	// --- 소셜 로그인 관련 필드 ---
	private String socialType;
	private String snsId;

	@Builder
	public MeetUserDto(String email, String password, Boolean terms, String name, String profileImgUrl, String job, String position, String socialType, String snsId) {
		this.email = email;
		this.password = password;
		this.terms = terms;
		this.name = name;
		this.profileImgUrl = profileImgUrl;
		this.job = job;
		this.position = position;
		this.socialType = socialType;
		this.snsId = snsId;
	}

    /**
     * Entity 객체를 DTO 객체로 변환하는 정적 팩토리 메서드.
     * - Service 계층에서 DB로부터 조회한 Entity를 클라이언트에 보낼 DTO로 변환할 때 사용합니다.
     * - 비밀번호와 같은 민감 정보는 제외하고, 안전한 데이터만 담습니다.
     */
    public static MeetUserDto fromEntity(MeetUser user) {
        return MeetUserDto.builder()
                .email(user.getEmail())
                .name(user.getName())
                .profileImgUrl(user.getProfileImgUrl())
                .job(user.getJob().name()) // Enum -> String 변환
                .position(user.getPosition().name()) // Enum -> String 변환
                .socialType(user.getSocialType())
                .snsId(user.getSnsId())
                .build();
	}

}
