package vibe.scon.scon_backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import vibe.scon.scon_backend.dto.store.StoreRequestDto;
import vibe.scon.scon_backend.entity.Schedule;
import vibe.scon.scon_backend.entity.enums.ScheduleStatus;
import vibe.scon.scon_backend.repository.ScheduleRepository;

import java.time.DayOfWeek;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 온보딩 플로우 통합 테스트.
 * 
 * <p>회원가입 → 매장 생성 전체 플로우를 검증합니다.</p>
 * 
 * <h3>테스트 케이스 추적:</h3>
 * <ul>
 *   <li>INTG-ONBOARDING-001: 회원가입 → 매장 생성 전체 플로우 성공</li>
 *   <li>INTG-ONBOARDING-002: HttpOnly Cookie 연속 사용 테스트</li>
 *   <li>INTG-ONBOARDING-003: 매장 생성 시 Draft 스케줄 자동 생성 확인</li>
 *   <li>INTG-ONBOARDING-004: Cookie 기반 인증으로 매장 생성 성공</li>
 * </ul>
 * 
 * <h3>요구사항 추적 (Traceability):</h3>
 * <ul>
 *   <li>{@code INTG-BE-Phase2-v1.1.0} - 온보딩 플로우 연동 (백엔드 작업 계획)</li>
 *   <li>{@code REQ-FUNC-001} - 3단계 온보딩 마법사 (회원가입, 로그인, 매장 생성)</li>
 * </ul>
 * 
 * @see <a href="../../../../../../SCON-Docs/BE-FE_Integration/INTG-BE-Phase2-signup.md">INTG-BE-Phase2-signup.md</a>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("온보딩 플로우 통합 테스트")
class OnboardingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Test
    @DisplayName("INTG-ONBOARDING-001: 회원가입 → 매장 생성 전체 플로우 성공")
    void onboarding_completeFlow_success() throws Exception {
        // Step 1: 회원가입
        SignupRequestDto signupRequest = SignupRequestDto.builder()
                .email("onboarding@example.com")
                .password("Password123!")
                .name("온보딩테스트사장")
                .phone("010-1234-5678")
                .build();

        MvcResult signupResult = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("회원가입이 완료되었습니다"))
                .andExpect(jsonPath("$.data.ownerId").exists())
                .andExpect(jsonPath("$.data.email").value("onboarding@example.com"))
                // HttpOnly Cookie 확인
                .andExpect(header().exists("Set-Cookie"))
                .andReturn();

        // Cookie에서 accessToken 및 refreshToken 추출
        jakarta.servlet.http.Cookie[] cookies = signupResult.getResponse().getCookies();
        jakarta.servlet.http.Cookie accessTokenCookie = null;
        jakarta.servlet.http.Cookie refreshTokenCookie = null;

        if (cookies != null) {
            for (jakarta.servlet.http.Cookie cookie : cookies) {
                if ("accessToken".equals(cookie.getName())) {
                    accessTokenCookie = cookie;
                } else if ("refreshToken".equals(cookie.getName())) {
                    refreshTokenCookie = cookie;
                }
            }
        }

        // Cookie가 설정되었는지 확인
        assertThat(accessTokenCookie).isNotNull();
        assertThat(refreshTokenCookie).isNotNull();
        assertThat(accessTokenCookie.getValue()).isNotEmpty();
        assertThat(refreshTokenCookie.getValue()).isNotEmpty();

        // Step 2: 매장 생성 (Cookie 기반 인증)
        StoreRequestDto storeRequest = StoreRequestDto.builder()
                .name("온보딩테스트베이커리")
                .businessType("베이커리카페")
                .address("서울시 강남구 테헤란로 123")
                .openTime(LocalTime.of(9, 0))
                .closeTime(LocalTime.of(22, 0))
                .storeHoliday(DayOfWeek.MONDAY)
                .build();

        MvcResult storeResult = mockMvc.perform(post("/api/v1/stores")
                        .cookie(accessTokenCookie) // HttpOnly Cookie 사용
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(storeRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.message").value("매장이 생성되었습니다"))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.name").value("온보딩테스트베이커리"))
                .andExpect(jsonPath("$.data.businessType").value("베이커리카페"))
                .andExpect(jsonPath("$.data.storeHoliday").value("MONDAY"))
                .andReturn();

        // 매장 ID 추출
        Long storeId = objectMapper.readTree(storeResult.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        // Step 3: Draft 스케줄 자동 생성 확인
        java.util.List<Schedule> draftSchedules = scheduleRepository.findByStoreIdAndStatus(storeId, ScheduleStatus.DRAFT);
        assertThat(draftSchedules).hasSize(1);
        
        Schedule draftSchedule = draftSchedules.get(0);
        assertThat(draftSchedule.getStore().getId()).isEqualTo(storeId);
        assertThat(draftSchedule.getStatus()).isEqualTo(ScheduleStatus.DRAFT);
        assertThat(draftSchedule.getWeekStartDate()).isNotNull();
    }

    @Test
    @DisplayName("INTG-ONBOARDING-002: HttpOnly Cookie 연속 사용 테스트")
    void onboarding_cookieContinuousUsage_success() throws Exception {
        // Step 1: 회원가입
        SignupRequestDto signupRequest = SignupRequestDto.builder()
                .email("cookieflow@example.com")
                .password("Password123!")
                .name("쿠키플로우테스트")
                .phone("010-9876-5432")
                .build();

        MvcResult signupResult = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        // Cookie 추출
        jakarta.servlet.http.Cookie[] cookies = signupResult.getResponse().getCookies();
        jakarta.servlet.http.Cookie accessTokenCookie = null;
        
        if (cookies != null) {
            for (jakarta.servlet.http.Cookie cookie : cookies) {
                if ("accessToken".equals(cookie.getName())) {
                    accessTokenCookie = cookie;
                    break;
                }
            }
        }

        assertThat(accessTokenCookie).isNotNull();

        // Step 2: Cookie로 매장 생성
        StoreRequestDto storeRequest = StoreRequestDto.builder()
                .name("쿠키플로우매장")
                .businessType("카페")
                .build();

        mockMvc.perform(post("/api/v1/stores")
                        .cookie(accessTokenCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(storeRequest)))
                .andExpect(status().isCreated());

        // Step 3: 같은 Cookie로 매장 목록 조회
        mockMvc.perform(get("/api/v1/stores")
                        .cookie(accessTokenCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data", isA(java.util.List.class)));
    }

    @Test
    @DisplayName("INTG-ONBOARDING-003: 매장 생성 시 Draft 스케줄 자동 생성 확인")
    void onboarding_autoCreateDraftSchedule_success() throws Exception {
        // Step 1: 회원가입
        SignupRequestDto signupRequest = SignupRequestDto.builder()
                .email("scheduleauto@example.com")
                .password("Password123!")
                .name("스케줄자동생성테스트")
                .phone("010-5555-6666")
                .build();

        MvcResult signupResult = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        // Cookie 추출
        jakarta.servlet.http.Cookie[] cookies = signupResult.getResponse().getCookies();
        jakarta.servlet.http.Cookie accessTokenCookie = null;
        
        if (cookies != null) {
            for (jakarta.servlet.http.Cookie cookie : cookies) {
                if ("accessToken".equals(cookie.getName())) {
                    accessTokenCookie = cookie;
                    break;
                }
            }
        }

        // Step 2: 매장 생성
        StoreRequestDto storeRequest = StoreRequestDto.builder()
                .name("스케줄자동생성매장")
                .businessType("베이커리")
                .build();

        MvcResult storeResult = mockMvc.perform(post("/api/v1/stores")
                        .cookie(accessTokenCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(storeRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        Long storeId = objectMapper.readTree(storeResult.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        // Step 3: Draft 스케줄 자동 생성 확인
        java.util.List<Schedule> draftSchedules = scheduleRepository.findByStoreIdAndStatus(storeId, ScheduleStatus.DRAFT);
        
        assertThat(draftSchedules).hasSize(1);
        
        Schedule draftSchedule = draftSchedules.get(0);
        assertThat(draftSchedule.getStatus()).isEqualTo(ScheduleStatus.DRAFT);
        assertThat(draftSchedule.getWeekStartDate()).isNotNull();
        
        // 현재 주차의 Draft 스케줄인지 확인 (매장 생성 시 자동 생성되는 것)
        java.time.LocalDate weekStartDate = draftSchedule.getWeekStartDate();
        
        // 주 시작일이 월요일이어야 함 (매장 생성 시 자동 생성되는 스케줄의 기준)
        assertThat(weekStartDate.getDayOfWeek()).isEqualTo(java.time.DayOfWeek.MONDAY);
    }

    @Test
    @DisplayName("INTG-ONBOARDING-004: Cookie 기반 인증으로 매장 생성 성공")
    void onboarding_storeCreationWithCookieAuth_success() throws Exception {
        // Step 1: 회원가입
        SignupRequestDto signupRequest = SignupRequestDto.builder()
                .email("cookieauth@example.com")
                .password("Password123!")
                .name("쿠키인증테스트")
                .phone("010-7777-8888")
                .build();

        MvcResult signupResult = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated())
                // Set-Cookie 헤더 존재 확인 (ResponseCookie 사용 시 여러 Cookie가 하나의 헤더에 포함될 수 있음)
                .andExpect(header().exists("Set-Cookie"))
                .andReturn();

        // Cookie 추출 및 검증
        jakarta.servlet.http.Cookie[] cookies = signupResult.getResponse().getCookies();
        assertThat(cookies).isNotNull();
        // accessToken과 refreshToken Cookie가 모두 설정되었는지 확인
        boolean hasAccessToken = false;
        boolean hasRefreshToken = false;

        jakarta.servlet.http.Cookie accessTokenCookie = null;
        for (jakarta.servlet.http.Cookie cookie : cookies) {
            if ("accessToken".equals(cookie.getName())) {
                hasAccessToken = true;
                accessTokenCookie = cookie;
            } else if ("refreshToken".equals(cookie.getName())) {
                hasRefreshToken = true;
            }
        }

        // accessToken과 refreshToken Cookie가 모두 설정되었는지 확인
        assertThat(hasAccessToken).isTrue();
        assertThat(hasRefreshToken).isTrue();
        assertThat(accessTokenCookie).isNotNull();
        assertThat(accessTokenCookie.getValue()).isNotEmpty();

        // Step 2: Cookie로 매장 생성 (Bearer Token 헤더 없이 Cookie만 사용)
        StoreRequestDto storeRequest = StoreRequestDto.builder()
                .name("쿠키인증매장")
                .businessType("카페")
                .address("서울시 서초구 서초대로 123")
                .build();

        mockMvc.perform(post("/api/v1/stores")
                        .cookie(accessTokenCookie) // Cookie만 사용 (Bearer Token 헤더 없음)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(storeRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.name").value("쿠키인증매장"))
                .andExpect(jsonPath("$.data.businessType").value("카페"));
    }
}
