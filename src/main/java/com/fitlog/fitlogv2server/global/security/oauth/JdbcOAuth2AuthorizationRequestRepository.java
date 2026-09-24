package com.fitlog.fitlogv2server.global.security.oauth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * OAuth2 인가 요청(state 등)을 인스턴스 메모리 세션 대신 DB에 저장한다.
 * 로그인 시작과 콜백이 서로 다른 인스턴스로 가거나, 그 사이에 재시작돼도 로그인이 실패하지 않는다.
 * - state 값을 키로 저장하고, 콜백에서 한 번만 꺼낼 수 있다 (DELETE ... RETURNING).
 * - 10분이 지나면 만료된다.
 * - 직렬화 데이터는 서버가 쓴 것만 DB에서 읽으므로 클라이언트가 조작할 수 없다.
 */
@Component
@RequiredArgsConstructor
public class JdbcOAuth2AuthorizationRequestRepository implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private static final Duration TTL = Duration.ofMinutes(10);

    private final JdbcTemplate jdbcTemplate;

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        String state = request.getParameter(OAuth2ParameterNames.STATE);
        if (state == null) {
            return null;
        }
        List<byte[]> rows = jdbcTemplate.query(
                "SELECT payload FROM oauth2_authorization_request WHERE state = ? AND expires_at > now()",
                (rs, rowNum) -> rs.getBytes("payload"), state);
        return rows.isEmpty() ? null : deserialize(rows.get(0));
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
                                         HttpServletRequest request, HttpServletResponse response) {
        if (authorizationRequest == null) {
            removeAuthorizationRequest(request, response);
            return;
        }
        jdbcTemplate.update("DELETE FROM oauth2_authorization_request WHERE expires_at <= now()");
        jdbcTemplate.update(
                "INSERT INTO oauth2_authorization_request (state, payload, expires_at) VALUES (?, ?, ?)",
                authorizationRequest.getState(), serialize(authorizationRequest), Timestamp.from(Instant.now().plus(TTL)));
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request, HttpServletResponse response) {
        String state = request.getParameter(OAuth2ParameterNames.STATE);
        if (state == null) {
            return null;
        }
        // 조회와 삭제를 한 문장으로 처리 → 같은 state 로 두 번 콜백해도 한 번만 성공
        List<byte[]> rows = jdbcTemplate.query(
                "DELETE FROM oauth2_authorization_request WHERE state = ? AND expires_at > now() RETURNING payload",
                (rs, rowNum) -> rs.getBytes("payload"), state);
        return rows.isEmpty() ? null : deserialize(rows.get(0));
    }

    private static byte[] serialize(OAuth2AuthorizationRequest request) {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(request);
            out.flush();
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("OAuth2 인가 요청 직렬화 실패", e);
        }
    }

    private static OAuth2AuthorizationRequest deserialize(byte[] payload) {
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(payload))) {
            return (OAuth2AuthorizationRequest) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            // 배포로 클래스 구조가 바뀐 경우 등: 인가 요청이 없는 것으로 처리 → 사용자가 로그인을 다시 시작
            return null;
        }
    }
}
