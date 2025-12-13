package com.age.b2b.config.auth;

import com.age.b2b.domain.Admin;
import com.age.b2b.domain.Client;
import com.age.b2b.domain.common.ClientStatus;
import lombok.Getter;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;

@Getter
public class PrincipalDetails implements UserDetails {

    private Admin admin;
    private Client client;

    // 1. 관리자 로그인 시 사용하는 생성자
    public PrincipalDetails(Admin admin) {
        this.admin = admin;
    }

    // 2. 고객사 로그인 시 사용하는 생성자
    public PrincipalDetails(Client client) {
        this.client = client;
    }

    // 권한(Role) 반환
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Collection<GrantedAuthority> collection = new ArrayList<>();

        if (admin != null) {
            // 관리자는 DB에 저장된 Role (ROLE_MASTER, ROLE_MANAGER) 그대로 사용
            collection.add(new SimpleGrantedAuthority(admin.getRole().name()));
        } else if (client != null) {
            // 고객사는 별도 Role 컬럼이 없다면 무조건 ROLE_CLIENT 부여
            collection.add(new SimpleGrantedAuthority("ROLE_CLIENT"));
        }
        return collection;
    }

    // 비밀번호 반환
    @Override
    public String getPassword() {
        return (admin != null) ? admin.getPassword() : client.getPassword();
    }

    // 아이디(Username) 반환
    @Override
    public String getUsername() {
        return (admin != null) ? admin.getUsername() : client.getUsername();
    }

    // 계정 만료 여부 (true: 만료 안 됨)
    @Override
    public boolean isAccountNonLocked() {
        return true; // 여기서 검사 안 함
    }

    @Override
    public boolean isEnabled() {
        return true; // 여기서 검사 안 함
    }

    // 2. 비밀번호가 맞은 후에 실행되는 사후 체크(Post-Check)에서 상태 검사
    @Override
    public boolean isCredentialsNonExpired() {
        if (client != null) {
            if (client.getApprovalStatus() == ClientStatus.WAITING) {
                // 비밀번호는 맞았는데, 상태가 WAITING이면 예외 발생
                throw new DisabledException("아직 승인 대기 중인 계정입니다. 관리자 승인을 기다려주세요.");
            }
            if (client.getApprovalStatus() == ClientStatus.REJECTED) {
                // 비밀번호는 맞았는데, 상태가 REJECTED이면 예외 발생
                throw new LockedException("가입이 거절된 계정입니다. 관리자에게 문의하세요.");
            }
        }
        return true; // 문제 없으면 통과
    }
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

}