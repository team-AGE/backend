package com.age.b2b.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ClientJoinDto {

    @NotBlank(message = "아이디는 필수입니다.")
    private String username;

    @NotBlank(message = "비밀번호는 필수입니다.")
    private String password;

    @NotBlank(message = "업태/업종은 필수입니다.")
    private String clientCategory; // 약국, 편의점 등

    @NotBlank(message = "상호명은 필수입니다.")
    private String businessName;

    @NotBlank(message = "사업자 번호는 필수입니다.")
    private String businessNumber;

    @NotBlank(message = "대표자명은 필수입니다.")
    private String ownerName;

    @NotBlank(message = "전화번호는 필수입니다.")
    private String phone;

    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    private String email;

    @NotBlank(message = "주소는 필수입니다.")
    private String address;
}
