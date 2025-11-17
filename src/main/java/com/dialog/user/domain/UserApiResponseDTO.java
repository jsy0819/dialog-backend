package com.dialog.user.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserApiResponseDTO {
    private boolean success;
    private String message;
}