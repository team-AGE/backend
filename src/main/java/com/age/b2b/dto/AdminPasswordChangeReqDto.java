package com.age.b2b.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AdminPasswordChangeReqDto {

    private String currentPassword;
    private String newPassword;
}
