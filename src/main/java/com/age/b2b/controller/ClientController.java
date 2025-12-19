package com.age.b2b.controller;

import com.age.b2b.dto.ClientSignupDto;
import com.age.b2b.service.ClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType; // MediaType import 필요
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/client")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;

    // 중복 확인
    @GetMapping("/check")
    public ResponseEntity<Boolean> checkDuplicate(@RequestParam String type, @RequestParam String value) {
        return ResponseEntity.ok(clientService.checkDuplicate(type, value));
    }

    // 회원가입
    @PostMapping(value = "/signup", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> signup(@ModelAttribute ClientSignupDto dto) {
        try {
            clientService.signup(dto);
            return ResponseEntity.ok("회원가입 요청이 완료되었습니다.");
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("파일 업로드 실패: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("가입 실패: " + e.getMessage());
        }
    }
}