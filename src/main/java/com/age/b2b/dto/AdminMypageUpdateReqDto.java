package com.age.b2b.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AdminMypageUpdateReqDto {

    private String email;
    private String phone;

    @Getter
    @NoArgsConstructor
    public static class AdminPasswordChangeReqDto {

        private String currentPassword;
        private String newPassword;
    }
}
