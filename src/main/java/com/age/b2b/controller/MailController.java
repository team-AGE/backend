package com.age.b2b.controller;

import com.age.b2b.service.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/mail")
@RequiredArgsConstructor
public class MailController {

    private final MailService mailService;

    // 인증 코드 전송
    @PostMapping("/send")
    public ResponseEntity<String> sendCode(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        mailService.sendVerifyMail(email);
        return ResponseEntity.ok("인증 코드가 전송되었습니다.");
    }

    // 인증 코드 확인
    @PostMapping("/verify")
    public ResponseEntity<Boolean> verifyCode(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String code = body.get("code");
        boolean isVerified = mailService.verifyCode(email, code);
        return ResponseEntity.ok(isVerified);
    }
}