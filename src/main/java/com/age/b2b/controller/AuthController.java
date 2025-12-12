package com.age.b2b.controller;

import com.age.b2b.dto.ClientJoinDto;
import com.age.b2b.service.ClientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final ClientService clientService;

    @PostMapping("/join/client")
    public ResponseEntity<String> joinClient(@Valid @RequestBody ClientJoinDto dto) {
        clientService.join(dto);
        return ResponseEntity.ok("회원가입 요청이 완료되었습니다. 관리자 승인을 기다려주세요.");
    }
}
