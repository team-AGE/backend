package com.age.b2b.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MypageResDto {

    private String username; // 아이디
    private String name; // 담당자명
    private String password;
    private String email;
    private String phone;
}
