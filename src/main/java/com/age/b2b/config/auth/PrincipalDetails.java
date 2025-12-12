package com.age.b2b.config.auth;

import com.age.b2b.domain.Admin;
import com.age.b2b.domain.Client;
import lombok.Getter;
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
    public boolean isAccountNonExpired() {
        return true;
    }

    // 계정 잠김 여부 (true: 안 잠김)
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    // 비밀번호 만료 여부 (true: 만료 안 됨)
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    // 계정 활성화 여부 (true: 활성화)
    @Override
    public boolean isEnabled() {
        // 필요하다면 여기서 고객사 승인 상태(APPROVED)를 체크해서 false를 리턴할 수도 있음
        // 하지만 우리는 Service에서 미리 체크하고 예외를 던지는 방식을 썼으므로 여기선 true
        return true;
    }


}