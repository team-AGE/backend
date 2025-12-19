package com.age.b2b.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@ToString
public class ClientSignupDto {
    private String username;
    private String password;
    private String businessName;
    private String businessNumber;
    private String ownerName;
    private String phone;
    private String email;
    private String zipCode;
    private String address;
    private String detailAddress;
    private String clientCategory;

    private MultipartFile file;
}