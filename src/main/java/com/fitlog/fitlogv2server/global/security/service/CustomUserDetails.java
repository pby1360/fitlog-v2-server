package com.fitlog.fitlogv2server.global.security.service;

import com.fitlog.fitlogv2server.domain.member.entity.Member;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Getter
public class CustomUserDetails implements UserDetails {

    private Long id;
    private String email;
    private String nickname;
    private String role;

    public CustomUserDetails(Long id, String email, String nickname, String role) {
        this.id = id;
        this.email = email;
        this.nickname = nickname;
        this.role = role;
    }

    public static CustomUserDetails from(Member member) {
        return new CustomUserDetails(
                member.getId(),
                member.getEmail(),
                member.getNickname(),
                member.getRole().getKey()
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority(this.role));
    }

    @Override
    public String getPassword() {
        return null; // OAuth2 로그인에서는 비밀번호를 사용하지 않음
    }

    @Override
    public String getUsername() {
        return this.email; // Spring Security에서 사용자 식별자로 사용될 값 (여기서는 이메일)
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
