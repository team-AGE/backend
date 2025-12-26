package com.age.b2b.service;

import com.age.b2b.domain.Admin;
import com.age.b2b.dto.AdminMypageUpdateReqDto;
import com.age.b2b.dto.AdminPasswordChangeReqDto;
import com.age.b2b.dto.MypageResDto;
import com.age.b2b.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.apache.catalina.security.SecurityUtil;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;



@Service
@RequiredArgsConstructor
public class AdminMypageService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    // 조회
    public MypageResDto getMypage() {

       String username = getCurrentUsername(); // 이미 쓰고 있다면 유지

        Admin admin = adminRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("관리자를 찾을 수 없습니다."));

        return new MypageResDto(
                admin.getUsername(),
                admin.getName(),
                "************",
                admin.getEmail(),
                admin.getPhone()
        );
    }
    // 수정
    @Transactional
    public void updateMypage(AdminMypageUpdateReqDto dto) {

        String username = getCurrentUsername(); // ← 여기

        Admin admin = adminRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("관리자를 찾을 수 없습니다."));

        admin.setEmail(dto.getEmail());
        admin.setPhone(dto.getPhone());
    }

    private String getCurrentUsername() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
                authentication.getName() == null ||
                "anonymousUser".equals(authentication.getName())) {
            throw new IllegalStateException("로그인된 관리자 정보가 없습니다.");
        }

        return authentication.getName();
    }
    // 비밀번호 변경
    public void changePassword(AdminPasswordChangeReqDto dto) {

        String username = getCurrentUsername();

        Admin admin = adminRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("관리자를 찾을 수 없습니다."));

        if (!passwordEncoder.matches(dto.getCurrentPassword(), admin.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }

        admin.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        adminRepository.save(admin); // ✅ 핵심
    }


}
