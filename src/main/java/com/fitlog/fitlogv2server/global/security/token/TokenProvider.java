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
    public String createAccessToken(String email, Role role) {
        return createToken(email, role, accessTokenExpiry);
    }

    // [2] Refresh Token 생성
    public String createRefreshToken(String email, Role role) {
        return createToken(email, role, refreshTokenExpiry);
    }

    // [3] 토큰 생성 공통 로직
    private String createToken(String email, Role role, long expiry) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiry);

        return Jwts.builder()
                .setSubject(email)
                .claim("role", role.getKey()) // 사용자 권한
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    // [4] 토큰에서 이메일(Subject) 추출 (API 인증 시 사용)
    public String getEmailFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    // [5] 토큰 유효성 검증 (API 인증 시 사용)
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            // (log) MalformedJwtException, ExpiredJwtException 등
            return false;
        }
    }
}