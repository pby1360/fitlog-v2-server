package com.fitlog.fitlogv2server.domain.member.repository;

import com.fitlog.fitlogv2server.domain.member.entity.Member;
import com.fitlog.fitlogv2server.domain.member.entity.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    /**
     * OAuth2 로그인 시 회원 식별. 이메일은 공급자 쪽에서 바뀌거나 공급자 간에 겹칠 수 있으므로
     * (공급자, 공급자 계정 ID) 로만 찾는다.
     */
    Optional<Member> findByProviderAndProviderId(Provider provider, String providerId);

    // 다른 공급자로 이미 가입된 이메일인지 확인 (자동 계정 합병은 하지 않음)
    Optional<Member> findByEmail(String email);
}
