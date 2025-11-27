package com.dialog.user.domain;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LoginDto {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String password;
    
    // 아이디 기억하기 체크 여부 추가
    private boolean rememberId; 
    
    // 직무 설정 여부 확인 
    private boolean needJobSetup;
    
}
