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
import java.util.Optional;
import java.util.UUID;

@Component
public class TokenProvider {

    // 토큰 용도 구분 클레임. 용도가 없는 (이전 버전) 토큰은 어떤 경로에서도 허용하지 않는다.
    public static final String CLAIM_TOKEN_USE = "token_use";
    public static final String TOKEN_USE_ACCESS = "access";
    public static final String TOKEN_USE_REFRESH = "refresh";

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

    // [1] Access Token 생성: 일반 API 인증에 필요한 사용자 정보를 담는다
    public String createAccessToken(Long memberId, String email, String nickname, Role role) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(String.valueOf(memberId)) // memberId를 subject로 사용
                .claim(CLAIM_TOKEN_USE, TOKEN_USE_ACCESS)
                .claim("email", email)
                .claim("nickname", nickname)
                .claim("role", role.getKey()) // 사용자 권한
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + accessTokenExpiry))
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    // [2] Refresh Token 생성: 재발급 용도로만 쓰이므로 사용자 정보를 담지 않는다
    public String createRefreshToken(Long memberId) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(String.valueOf(memberId))
                .claim(CLAIM_TOKEN_USE, TOKEN_USE_REFRESH)
                .setId(UUID.randomUUID().toString()) // 같은 시각에 발급해도 토큰 값이 겹치지 않도록
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + refreshTokenExpiry))
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    // 토큰에서 Claims(정보 단위)를 추출한다. 서명/만료 검증에 실패하면 예외가 발생한다.
    public Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // 유효한 Access Token이면 Claims 반환 (Refresh/용도 없는 토큰은 거부)
    public Optional<Claims> parseAccessToken(String token) {
        return parse(token, TOKEN_USE_ACCESS);
    }

    // 유효한 Refresh Token이면 Claims 반환 (Access/용도 없는 토큰은 거부)
    public Optional<Claims> parseRefreshToken(String token) {
        return parse(token, TOKEN_USE_REFRESH);
    }

    private Optional<Claims> parse(String token, String expectedUse) {
        try {
            Claims claims = getClaims(token);
            if (!expectedUse.equals(claims.get(CLAIM_TOKEN_USE, String.class))) {
                return Optional.empty();
            }
            return Optional.of(claims);
        } catch (Exception e) {
            // MalformedJwtException, ExpiredJwtException, SignatureException 등
            return Optional.empty();
        }
    }
}
