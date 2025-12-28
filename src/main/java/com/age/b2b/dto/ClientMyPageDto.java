package com.age.b2b.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// 마이페이지 조회를 위한 DTO
public class ClientMyPageDto {
    private String type;
    private String company;
    private String number;
    private String ceo;
    private String phone;
    private String username;
    private String email;
    private String address;
    private String detailAddress;
    private String bizFileName;
}
