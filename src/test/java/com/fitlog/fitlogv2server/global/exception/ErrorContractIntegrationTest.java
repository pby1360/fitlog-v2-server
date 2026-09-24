package com.fitlog.fitlogv2server.global.exception;

import com.fitlog.fitlogv2server.domain.member.entity.Member;
import com.fitlog.fitlogv2server.domain.member.entity.Role;
import com.fitlog.fitlogv2server.domain.workoutprogram.entity.WorkoutProgram;
import com.fitlog.fitlogv2server.global.security.token.TokenProvider;
import com.fitlog.fitlogv2server.support.IntegrationTestSupport;
import com.fitlog.fitlogv2server.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP 계층까지 포함해 오류 응답 형식({code, message, requestId})과 상태 코드를 확인한다.
 */
@AutoConfigureMockMvc
class ErrorContractIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private TokenProvider tokenProvider;
    @Autowired
    private TestFixtures fixtures;

    @Test
    void missingToken_returns401WithErrorBodyAndRequestId() throws Exception {
        mockMvc.perform(get("/api/members/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.requestId", notNullValue()))
                .andExpect(header().exists("X-Request-Id"));
    }

    @Test
    void safeClientRequestId_isEchoed() throws Exception {
        mockMvc.perform(get("/api/members/me").header("X-Request-Id", "client-req-12345"))
                .andExpect(header().string("X-Request-Id", "client-req-12345"))
                .andExpect(jsonPath("$.requestId").value("client-req-12345"));
    }

    @Test
    void otherMembersProgram_isIndistinguishableFromMissing() throws Exception {
        Member owner = fixtures.member();
        Member other = fixtures.member();
        WorkoutProgram program = fixtures.emptyProgram(owner);
        String body = "{\"name\":\"탈취\",\"description\":\"\",\"parts\":[]}";

        mockMvc.perform(put("/api/workout-programs/" + program.getId())
                        .header("Authorization", "Bearer " + accessToken(other))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        mockMvc.perform(put("/api/workout-programs/999999999")
                        .header("Authorization", "Bearer " + accessToken(other))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void invalidBody_returnsValidationFailed() throws Exception {
        Member member = fixtures.member();

        mockMvc.perform(put("/api/workout-programs/1")
                        .header("Authorization", "Bearer " + accessToken(member))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void malformedJson_returnsBadRequestWithoutInternalDetails() throws Exception {
        Member member = fixtures.member();

        mockMvc.perform(put("/api/workout-programs/1")
                        .header("Authorization", "Bearer " + accessToken(member))
                        .contentType(MediaType.APPLICATION_JSON).content("{not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("요청 형식이 올바르지 않습니다."));
    }

    @Test
    void healthEndpoint_isPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    private String accessToken(Member member) {
        return tokenProvider.createAccessToken(member.getId(), member.getEmail(), member.getNickname(), Role.USER);
    }
}
