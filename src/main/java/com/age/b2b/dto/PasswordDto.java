package com.age.b2b.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// 마이페이지에서 패스워드 변경을 위한 DTO
public class PasswordDto {
    private String currentPassword;
    private String newPassword;
    private String newPasswordConfirm;
}
