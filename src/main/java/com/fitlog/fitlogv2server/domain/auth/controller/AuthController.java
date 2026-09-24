package com.fitlog.fitlogv2server.domain.auth.controller;

import com.fitlog.fitlogv2server.domain.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refresh(@RequestBody Map<String, String> body) {
        AuthService.TokenPair tokens = authService.reissue(body.get("refreshToken"));
        return ResponseEntity.ok(Map.of(
                "accessToken", tokens.accessToken(),
                "refreshToken", tokens.refreshToken()
        ));
    }

    // Access Token이 만료된 상태에서도 로그아웃할 수 있도록 Refresh Token으로 식별한다
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody(required = false) Map<String, String> body) {
        authService.logout(body != null ? body.get("refreshToken") : null);
        return ResponseEntity.noContent().build();
    }
}
