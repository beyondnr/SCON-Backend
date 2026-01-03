package vibe.scon.scon_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import vibe.scon.scon_backend.dto.auth.LoginRequestDto;
import vibe.scon.scon_backend.dto.auth.RefreshTokenRequestDto;
import vibe.scon.scon_backend.dto.auth.SignupRequestDto;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AuthController 통합 테스트.
 * 
 * <h3>테스트 케이스 추적:</h3>
 * <ul>
 *   <li>TC-AUTH-001: 회원가입 성공</li>
 *   <li>TC-AUTH-003: 중복 이메일 가입 실패 (409)</li>
 *   <li>TC-AUTH-004: 로그인 성공 + JWT 발급</li>
 *   <li>TC-AUTH-005: 잘못된 비밀번호 로그인 실패 (401)</li>
 *   <li>TC-AUTH-006: 토큰 갱신 API 성공</li>
 *   <li>TC-AUTH-010: 로그아웃 API 성공</li>
 *   <li>TC-AUTH-011: 로그아웃 후 토큰 갱신 실패</li>
 *   <li>TC-AUTH-012: 유효하지 않은 토큰으로 로그아웃 실패</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("AuthController 통합 테스트")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private SignupRequestDto signupRequest;

    @BeforeEach
    void setUp() {
        signupRequest = SignupRequestDto.builder()
                .email("integrationtest@example.com")
                .password("Password123!")
                .name("테스트사장")
                .phone("010-9999-8888")
                .build();
    }

    @Test
    @DisplayName("TC-AUTH-001: 회원가입 API 성공")
    void signup_success() throws Exception {
        // When & Then - 회원가입은 201 Created 반환
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.ownerId").exists())
                .andExpect(jsonPath("$.data.email").value("integrationtest@example.com"))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
    }

    @Test
    @DisplayName("TC-AUTH-003: 중복 이메일 가입 실패")
    void signup_duplicateEmail_returns400() throws Exception {
        // Given - 먼저 회원가입
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated());

        // When & Then - 같은 이메일로 다시 가입 시도
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("이미 사용 중인 이메일")));
    }

    @Test
    @DisplayName("TC-AUTH-004: 로그인 API 성공")
    void login_success() throws Exception {
        // Given - 먼저 회원가입
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated());

        // When & Then - 로그인
        LoginRequestDto loginRequest = LoginRequestDto.builder()
                .email("integrationtest@example.com")
                .password("Password123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.ownerId").exists())
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists());
    }

    @Test
    @DisplayName("TC-AUTH-005: 잘못된 비밀번호 로그인 실패")
    void login_wrongPassword_returns400() throws Exception {
        // Given - 먼저 회원가입
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated());

        // When & Then - 잘못된 비밀번호로 로그인
        LoginRequestDto loginRequest = LoginRequestDto.builder()
                .email("integrationtest@example.com")
                .password("WrongPassword!")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("비밀번호가 일치하지 않습니다")));
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 이메일")
    void login_emailNotFound_returns400() throws Exception {
        // Given
        LoginRequestDto loginRequest = LoginRequestDto.builder()
                .email("nonexistent@example.com")
                .password("Password123!")
                .build();

        // When & Then
        // POC-BE-SEC-002: 이메일 존재 여부 노출 방지를 위해 400 BadRequest 반환
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("이메일 또는 비밀번호가 올바르지 않습니다")));
    }

    @Test
    @DisplayName("TC-AUTH-006: 토큰 갱신 API 성공")
    void refreshToken_success() throws Exception {
        // Given - 회원가입하여 토큰 획득
        MvcResult signupResult = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        // 응답에서 refreshToken 추출
        String responseBody = signupResult.getResponse().getContentAsString();
        String refreshToken = objectMapper.readTree(responseBody)
                .get("data").get("refreshToken").asText();

        // When & Then - 토큰 갱신
        RefreshTokenRequestDto refreshRequest = RefreshTokenRequestDto.builder()
                .refreshToken(refreshToken)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andReturn();
    }

    @Test
    @DisplayName("TC-AUTH-009: 유효하지 않은 Refresh Token으로 갱신 실패")
    void refreshToken_invalidToken_returns400() throws Exception {
        // Given
        RefreshTokenRequestDto refreshRequest = RefreshTokenRequestDto.builder()
                .refreshToken("invalid.refresh.token")
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("회원가입 요청 필수 필드 누락 시 검증 실패")
    void signup_missingRequiredFields_returns400() throws Exception {
        // Given - 이메일 누락
        SignupRequestDto invalidRequest = SignupRequestDto.builder()
                .password("Password123!")
                .name("테스트")
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TC-AUTH-010: 로그아웃 API 성공")
    void logout_success() throws Exception {
        // Given - 먼저 회원가입하여 Refresh Token 획득
        MvcResult signupResult = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        // 응답에서 refreshToken 추출
        String responseBody = signupResult.getResponse().getContentAsString();
        String refreshToken = objectMapper.readTree(responseBody)
                .get("data").get("refreshToken").asText();

        // When & Then - 로그아웃
        RefreshTokenRequestDto logoutRequest = RefreshTokenRequestDto.builder()
                .refreshToken(refreshToken)
                .build();

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("로그아웃이 완료되었습니다"));
    }

    @Test
    @DisplayName("TC-AUTH-011: 로그아웃 후 토큰 갱신 실패")
    void logout_thenRefreshToken_fails() throws Exception {
        // Given - 회원가입하여 Refresh Token 획득
        MvcResult signupResult = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = signupResult.getResponse().getContentAsString();
        String refreshToken = objectMapper.readTree(responseBody)
                .get("data").get("refreshToken").asText();

        // 로그아웃
        RefreshTokenRequestDto logoutRequest = RefreshTokenRequestDto.builder()
                .refreshToken(refreshToken)
                .build();

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isOk());

        // When & Then - 로그아웃한 토큰으로 갱신 시도 (실패해야 함)
        RefreshTokenRequestDto refreshRequest = RefreshTokenRequestDto.builder()
                .refreshToken(refreshToken)
                .build();

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("이미 로그아웃된 Refresh Token")));
    }

    @Test
    @DisplayName("TC-AUTH-012: 유효하지 않은 토큰으로 로그아웃 실패")
    void logout_invalidToken_returns400() throws Exception {
        // Given
        RefreshTokenRequestDto logoutRequest = RefreshTokenRequestDto.builder()
                .refreshToken("invalid.refresh.token")
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isBadRequest());
    }
}
