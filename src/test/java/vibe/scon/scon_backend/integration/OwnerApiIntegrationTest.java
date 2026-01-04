package vibe.scon.scon_backend.integration;

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
import vibe.scon.scon_backend.dto.auth.SignupRequestDto;
import vibe.scon.scon_backend.dto.owner.UpdateOwnerProfileRequest;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * OwnerController 통합 테스트.
 * 
 * <h3>테스트 케이스 추적:</h3>
 * <ul>
 *   <li>TC-OWNER-001: 프로필 조회 성공</li>
 *   <li>TC-OWNER-002: 프로필 수정 성공 (name만)</li>
 *   <li>TC-OWNER-003: 프로필 수정 성공 (phone만)</li>
 *   <li>TC-OWNER-004: 프로필 수정 성공 (name과 phone 모두)</li>
 *   <li>TC-OWNER-005: name을 빈 문자열로 설정 시도 (400 BadRequest)</li>
 *   <li>TC-OWNER-006: phone을 null로 설정 (빈 문자열 전달)</li>
 *   <li>TC-OWNER-009: 유효성 검증 실패 (name 100자 초과)</li>
 *   <li>TC-OWNER-010: 인증 토큰 없이 API 호출 시 401 응답</li>
 *   <li>TC-OWNER-011: 로그인 → 프로필 조회 → 프로필 수정 E2E 테스트</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("OwnerController 통합 테스트")
class OwnerApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String accessToken;
    private Long ownerId;

    @BeforeEach
    void setUp() throws Exception {
        // 회원가입하여 토큰 획득
        SignupRequestDto signupRequest = SignupRequestDto.builder()
                .email("ownertest@example.com")
                .password("Password123!")
                .name("사용자테스트")
                .phone("010-1111-2222")
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        // Cookie에서 accessToken 추출
        jakarta.servlet.http.Cookie[] cookies = result.getResponse().getCookies();
        if (cookies != null) {
            for (jakarta.servlet.http.Cookie cookie : cookies) {
                if ("accessToken".equals(cookie.getName())) {
                    accessToken = cookie.getValue();
                    break;
                }
            }
        }
        
        // Cookie에서 토큰을 찾지 못한 경우 응답 본문에서 추출 시도 (하위 호환성)
        String responseBody = result.getResponse().getContentAsString();
        if (accessToken == null) {
            try {
                accessToken = objectMapper.readTree(responseBody).get("data").get("accessToken").asText();
            } catch (Exception e) {
                // 토큰이 응답 본문에 없는 경우 (Cookie 방식)
                accessToken = null;
            }
        }
        
        ownerId = objectMapper.readTree(responseBody).get("data").get("ownerId").asLong();
    }

    @Test
    @DisplayName("TC-OWNER-001: 프로필 조회 성공")
    void getCurrentOwnerProfile_success() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/owners/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("사용자 정보 조회 성공"))
                .andExpect(jsonPath("$.data.ownerId").value(ownerId.intValue()))
                .andExpect(jsonPath("$.data.email").value("ownertest@example.com"))
                .andExpect(jsonPath("$.data.name").value("사용자테스트"))
                .andExpect(jsonPath("$.data.phone").value("010-1111-2222"))
                .andExpect(jsonPath("$.data.createdAt").exists())
                .andExpect(jsonPath("$.data.updatedAt").exists());
    }

    @Test
    @DisplayName("TC-OWNER-010: 인증 토큰 없이 API 호출 시 401 응답")
    void getCurrentOwnerProfile_unauthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/owners/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("TC-OWNER-002: 프로필 수정 성공 (name만)")
    void updateOwnerProfile_nameOnly_success() throws Exception {
        // Given
        UpdateOwnerProfileRequest request = new UpdateOwnerProfileRequest("홍길동 수정", null);

        // When & Then
        mockMvc.perform(patch("/api/v1/owners/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("사용자 정보가 수정되었습니다"))
                .andExpect(jsonPath("$.data.name").value("홍길동 수정"))
                .andExpect(jsonPath("$.data.phone").value("010-1111-2222")); // 변경되지 않음
    }

    @Test
    @DisplayName("TC-OWNER-003: 프로필 수정 성공 (phone만)")
    void updateOwnerProfile_phoneOnly_success() throws Exception {
        // Given
        UpdateOwnerProfileRequest request = new UpdateOwnerProfileRequest(null, "010-9876-5432");

        // When & Then
        mockMvc.perform(patch("/api/v1/owners/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.name").value("사용자테스트")) // 변경되지 않음
                .andExpect(jsonPath("$.data.phone").value("010-9876-5432"));
    }

    @Test
    @DisplayName("TC-OWNER-004: 프로필 수정 성공 (name과 phone 모두)")
    void updateOwnerProfile_bothFields_success() throws Exception {
        // Given
        UpdateOwnerProfileRequest request = new UpdateOwnerProfileRequest("홍길동 수정", "010-9876-5432");

        // When & Then
        mockMvc.perform(patch("/api/v1/owners/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.name").value("홍길동 수정"))
                .andExpect(jsonPath("$.data.phone").value("010-9876-5432"));
    }

    @Test
    @DisplayName("TC-OWNER-005: name을 빈 문자열로 설정 시도 (400 BadRequest)")
    void updateOwnerProfile_emptyName_throwsBadRequest() throws Exception {
        // Given
        UpdateOwnerProfileRequest request = new UpdateOwnerProfileRequest("", null);

        // When & Then
        mockMvc.perform(patch("/api/v1/owners/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("INVALID_NAME"))
                .andExpect(jsonPath("$.message").value(containsString("이름은 빈 문자열일 수 없습니다")));
    }

    @Test
    @DisplayName("TC-OWNER-006: phone을 null로 설정 (빈 문자열 전달)")
    void updateOwnerProfile_emptyPhone_setsNull() throws Exception {
        // Given
        UpdateOwnerProfileRequest request = new UpdateOwnerProfileRequest(null, "");

        // When & Then
        mockMvc.perform(patch("/api/v1/owners/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.phone").doesNotExist());
    }

    @Test
    @DisplayName("TC-OWNER-009: 유효성 검증 실패 (name 100자 초과)")
    void updateOwnerProfile_nameTooLong_throwsBadRequest() throws Exception {
        // Given
        String longName = "a".repeat(101); // 101자
        UpdateOwnerProfileRequest request = new UpdateOwnerProfileRequest(longName, null);

        // When & Then
        mockMvc.perform(patch("/api/v1/owners/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("TC-OWNER-011: 로그인 → 프로필 조회 → 프로필 수정 E2E 테스트")
    void e2e_login_getProfile_updateProfile() throws Exception {
        // Step 1: 프로필 조회
        mockMvc.perform(get("/api/v1/owners/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("사용자테스트"));

        // Step 2: 프로필 수정
        UpdateOwnerProfileRequest updateRequest = new UpdateOwnerProfileRequest("E2E 테스트 이름", "010-9999-8888");
        mockMvc.perform(patch("/api/v1/owners/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("E2E 테스트 이름"))
                .andExpect(jsonPath("$.data.phone").value("010-9999-8888"));

        // Step 3: 수정된 프로필 재조회
        mockMvc.perform(get("/api/v1/owners/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("E2E 테스트 이름"))
                .andExpect(jsonPath("$.data.phone").value("010-9999-8888"));
    }
}

