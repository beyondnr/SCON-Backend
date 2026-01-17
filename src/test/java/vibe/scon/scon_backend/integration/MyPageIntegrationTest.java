package vibe.scon.scon_backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import vibe.scon.scon_backend.dto.auth.SignupRequestDto;
import vibe.scon.scon_backend.dto.owner.UpdateOwnerProfileRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 마이페이지 플로우 통합 테스트.
 * 
 * <p>마이페이지에서 사용자 프로필 조회/수정 기능의 통합 테스트입니다.
 * 프로필 조회 → 수정 플로우, Cookie 기반 인증 연속 사용, 부분 수정, 유효성 검증을 검증합니다.</p>
 * 
 * <h3>테스트 케이스 추적:</h3>
 * <ul>
 *   <li>INTG-MYPAGE-001: 프로필 조회 → 수정 플로우 성공</li>
 *   <li>INTG-MYPAGE-002: Cookie 기반 인증 연속 사용 테스트</li>
 *   <li>INTG-MYPAGE-003: 부분 수정 테스트 (name만, phone만)</li>
 *   <li>INTG-MYPAGE-004: 유효성 검증 실패 테스트 (fieldErrors 포함 검증)</li>
 * </ul>
 * 
 * <h3>요구사항 추적 (Traceability):</h3>
 * <ul>
 *   <li>{@code INTG-BE-Phase5-v1.1.0} - 마이페이지 연동 (백엔드 작업 계획)</li>
 *   <li>{@code POC-BE-FUNC-002} - 마이페이지 사용자 프로필 조회/수정 API</li>
 * </ul>
 * 
 * @see <a href="../../../../../../SCON-Docs/BE-FE_Integration/INTG-BE-Phase5-mypage.md">INTG-BE-Phase5-mypage.md</a>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("마이페이지 플로우 통합 테스트")
class MyPageIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Set-Cookie 헤더에서 특정 쿠키 값 추출
     */
    private String extractCookieValue(java.util.List<String> setCookieHeaders, String cookieName) {
        for (String header : setCookieHeaders) {
            String[] parts = header.split(";");
            if (parts[0].startsWith(cookieName + "=")) {
                return parts[0].substring((cookieName + "=").length());
            }
        }
        return null;
    }

    @Test
    @DisplayName("INTG-MYPAGE-001: 프로필 조회 → 수정 플로우 성공")
    void mypage_profileGetAndUpdateFlow_success() throws Exception {
        // Step 1: 회원가입
        SignupRequestDto signupRequest = SignupRequestDto.builder()
                .email("mypage@example.com")
                .password("Password123!")
                .name("마이페이지테스트")
                .phone("010-1234-5678")
                .isAgreedToTerms(true)
                .build();

        MvcResult signupResult = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                .andReturn();

        // Cookie에서 accessToken 추출
        String accessTokenValue = extractCookieValue(
                signupResult.getResponse().getHeaders("Set-Cookie"),
                "accessToken"
        );
        assertThat(accessTokenValue).isNotNull();
        jakarta.servlet.http.Cookie accessTokenCookie = new jakarta.servlet.http.Cookie("accessToken", accessTokenValue);

        // Step 2: 프로필 조회
        MvcResult getProfileResult = mockMvc.perform(get("/api/v1/owners/me")
                        .cookie(accessTokenCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("사용자 정보 조회 성공"))
                .andExpect(jsonPath("$.data.email").value("mypage@example.com"))
                .andExpect(jsonPath("$.data.name").value("마이페이지테스트"))
                .andExpect(jsonPath("$.data.phone").value("010-1234-5678"))
                .andReturn();

        Long ownerId = objectMapper.readTree(getProfileResult.getResponse().getContentAsString())
                .get("data").get("ownerId").asLong();

        // Step 3: 프로필 수정
        UpdateOwnerProfileRequest updateRequest = new UpdateOwnerProfileRequest("수정된이름", "010-9876-5432");

        mockMvc.perform(patch("/api/v1/owners/me")
                        .cookie(accessTokenCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("사용자 정보가 수정되었습니다"))
                .andExpect(jsonPath("$.data.ownerId").value(ownerId.intValue()))
                .andExpect(jsonPath("$.data.name").value("수정된이름"))
                .andExpect(jsonPath("$.data.phone").value("010-9876-5432"));

        // Step 4: 수정된 프로필 재조회
        mockMvc.perform(get("/api/v1/owners/me")
                        .cookie(accessTokenCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("수정된이름"))
                .andExpect(jsonPath("$.data.phone").value("010-9876-5432"));
    }

    @Test
    @DisplayName("INTG-MYPAGE-002: Cookie 기반 인증 연속 사용 테스트")
    void mypage_cookieContinuousUsage_success() throws Exception {
        // Step 1: 회원가입
        SignupRequestDto signupRequest = SignupRequestDto.builder()
                .email("cookiecontinuous@example.com")
                .password("Password123!")
                .name("쿠키연속사용테스트")
                .phone("010-1111-2222")
                .isAgreedToTerms(true)
                .build();

        MvcResult signupResult = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        // Cookie 추출
        String accessTokenValue = extractCookieValue(
                signupResult.getResponse().getHeaders("Set-Cookie"),
                "accessToken"
        );
        jakarta.servlet.http.Cookie accessTokenCookie = new jakarta.servlet.http.Cookie("accessToken", accessTokenValue);

        // Step 2: Cookie를 사용하여 연속적으로 프로필 조회 및 수정
        // 2-1. 첫 번째 프로필 조회
        mockMvc.perform(get("/api/v1/owners/me")
                        .cookie(accessTokenCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("쿠키연속사용테스트"));

        // 2-2. 프로필 수정
        UpdateOwnerProfileRequest updateRequest = new UpdateOwnerProfileRequest("쿠키연속수정", null);

        mockMvc.perform(patch("/api/v1/owners/me")
                        .cookie(accessTokenCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("쿠키연속수정"));

        // 2-3. 두 번째 프로필 조회 (Cookie 재사용)
        mockMvc.perform(get("/api/v1/owners/me")
                        .cookie(accessTokenCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("쿠키연속수정"));
    }

    @Test
    @DisplayName("INTG-MYPAGE-003: 부분 수정 테스트 (name만, phone만)")
    void mypage_partialUpdate_success() throws Exception {
        // Step 1: 회원가입
        SignupRequestDto signupRequest = SignupRequestDto.builder()
                .email("partialupdate@example.com")
                .password("Password123!")
                .name("부분수정테스트")
                .phone("010-3333-4444")
                .isAgreedToTerms(true)
                .build();

        MvcResult signupResult = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String accessTokenValue = extractCookieValue(
                signupResult.getResponse().getHeaders("Set-Cookie"),
                "accessToken"
        );
        jakarta.servlet.http.Cookie accessTokenCookie = new jakarta.servlet.http.Cookie("accessToken", accessTokenValue);

        // Step 2: name만 수정
        UpdateOwnerProfileRequest nameOnlyRequest = new UpdateOwnerProfileRequest("이름만수정", null);

        mockMvc.perform(patch("/api/v1/owners/me")
                        .cookie(accessTokenCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nameOnlyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("이름만수정"))
                .andExpect(jsonPath("$.data.phone").value("010-3333-4444")); // 변경되지 않음

        // Step 3: phone만 수정
        UpdateOwnerProfileRequest phoneOnlyRequest = new UpdateOwnerProfileRequest(null, "010-5555-6666");

        mockMvc.perform(patch("/api/v1/owners/me")
                        .cookie(accessTokenCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(phoneOnlyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("이름만수정")) // 변경되지 않음
                .andExpect(jsonPath("$.data.phone").value("010-5555-6666"));
    }

    @Test
    @DisplayName("INTG-MYPAGE-004: 유효성 검증 실패 테스트 (fieldErrors 포함 검증)")
    void mypage_validationFailure_returnsFieldErrors() throws Exception {
        // Step 1: 회원가입
        SignupRequestDto signupRequest = SignupRequestDto.builder()
                .email("validationtest@example.com")
                .password("Password123!")
                .name("유효성검증테스트")
                .phone("010-7777-8888")
                .isAgreedToTerms(true)
                .build();

        MvcResult signupResult = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String accessTokenValue = extractCookieValue(
                signupResult.getResponse().getHeaders("Set-Cookie"),
                "accessToken"
        );
        jakarta.servlet.http.Cookie accessTokenCookie = new jakarta.servlet.http.Cookie("accessToken", accessTokenValue);

        // Step 2: 유효성 검증 실패 (name 100자 초과)
        String longName = "a".repeat(101);
        UpdateOwnerProfileRequest invalidRequest = new UpdateOwnerProfileRequest(longName, null);

        mockMvc.perform(patch("/api/v1/owners/me")
                        .cookie(accessTokenCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.fieldErrors[0].field").exists())
                .andExpect(jsonPath("$.fieldErrors[0].message").exists())
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("name")));

        // Step 3: 유효성 검증 실패 (phone 형식 오류)
        UpdateOwnerProfileRequest invalidPhoneRequest = new UpdateOwnerProfileRequest(null, "123-456-789");

        mockMvc.perform(patch("/api/v1/owners/me")
                        .cookie(accessTokenCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPhoneRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("phone")));
    }
}
