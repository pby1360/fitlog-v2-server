package com.fitlog.fitlogv2server.domain.member.repository;

import com.fitlog.fitlogv2server.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    /**
     * OAuth2 로그인 시, 제공받은 email을 통해
     * 이미 가입된 사용자인지 확인하기 위한 메서드입니다.
     */
    Optional<Member> findByEmail(String email);
}