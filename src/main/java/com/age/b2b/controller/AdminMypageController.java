package com.age.b2b.controller;

import com.age.b2b.dto.AdminMypageUpdateReqDto;
import com.age.b2b.dto.AdminPasswordChangeReqDto;
import com.age.b2b.dto.MypageResDto;
import com.age.b2b.service.AdminMypageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminMypageController {

    private final AdminMypageService adminMypageService;

    // 조회
    @GetMapping("/mypage")
    public MypageResDto getAdminMypage() {
        return adminMypageService.getMypage();
    }

    // 수정
    @PutMapping("/mypage")
    public void updateAdminMypage(
            @RequestBody AdminMypageUpdateReqDto dto
    ) {
        adminMypageService.updateMypage(dto);
    }
    // 비밀번호 수정
    @PutMapping("/mypage/password")
    public ResponseEntity<?> changePassword(
            @RequestBody AdminPasswordChangeReqDto dto
    ) {
        adminMypageService.changePassword(dto);
        return ResponseEntity.ok().build();
    }



}
