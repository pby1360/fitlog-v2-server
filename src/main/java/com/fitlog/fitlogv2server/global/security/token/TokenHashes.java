package com.fitlog.fitlogv2server.global.security.token;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * 토큰/코드 원문 대신 DB에 저장할 해시와, 추측 불가능한 난수 값을 만든다.
 */
public final class TokenHashes {

    private static final SecureRandom RANDOM = new SecureRandom();

    private TokenHashes() {
    }

    public static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    // URL에 그대로 실을 수 있는 256비트 난수
    public static String randomUrlSafe() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
