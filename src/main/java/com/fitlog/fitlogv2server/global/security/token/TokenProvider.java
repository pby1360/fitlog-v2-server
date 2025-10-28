package com.fitlog.fitlogv2server.global.security.token;

import com.fitlog.fitlogv2server.domain.member.entity.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class TokenProvider {

    private final Key key;
    private final long accessTokenExpiry;
    private final long refreshTokenExpiry;

    public TokenProvider(
            @Value("${jwt.secret}") String secretKey,
            @Value("${jwt.access-token-expiry}") long accessTokenExpiry,
            @Value("${jwt.refresh-token-expiry}") long refreshTokenExpiry) {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenExpiry = accessTokenExpiry * 1000; // yml은 '초' 단위, 여기선 '밀리초'
        this.refreshTokenExpiry = refreshTokenExpiry * 1000;
    }

    // [1] Access Token 생성
    public String createAccessToken(Long memberId, String email, String nickname, Role role) {
        return createToken(memberId, email, nickname, role, accessTokenExpiry);
    }

    // [2] Refresh Token 생성
    public String createRefreshToken(Long memberId, String email, String nickname, Role role) {
        return createToken(memberId, email, nickname, role, refreshTokenExpiry);
    }

    // [3] 토큰 생성 공통 로직
    private String createToken(Long memberId, String email, String nickname, Role role, long expiry) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiry);

        return Jwts.builder()
                .setSubject(String.valueOf(memberId)) // memberId를 subject로 사용
                .claim("email", email)
                .claim("nickname", nickname)
                .claim("role", role.getKey()) // 사용자 권한
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    // [수정] getEmailFromToken을 getClaims를 사용하도록 변경
    public String getEmailFromToken(String token) {
        return getClaims(token).getSubject();
    }

    // [신규] 토큰에서 Claims(정보 단위)를 추출하는 메서드
    public Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // [수정] validateToken (기존 메서드)
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            // (log) MalformedJwtException, ExpiredJwtException 등
            // log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }
}