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
import vibe.scon.scon_backend.dto.auth.SignupRequestDto;
import vibe.scon.scon_backend.dto.store.StoreRequestDto;
import vibe.scon.scon_backend.entity.Schedule;
import vibe.scon.scon_backend.entity.Store;
import vibe.scon.scon_backend.entity.enums.ScheduleStatus;
import vibe.scon.scon_backend.repository.ScheduleRepository;
import vibe.scon.scon_backend.repository.StoreRepository;

import jakarta.servlet.http.Cookie;
import java.time.LocalDate;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("ScheduleController 통합 테스트")
class ScheduleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private StoreRepository storeRepository;

    private Long storeId;
    private String accessToken;
    private Cookie accessTokenCookie;

    @BeforeEach
    void setUp() throws Exception {
        // 1. 회원가입 및 로그인
        SignupRequestDto signupRequest = SignupRequestDto.builder()
                .email("scheduletest@example.com")
                .password("Password123!")
                .name("스케줄테스터")
                .phone("010-1234-5678")
                .build();

        MvcResult authResult = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        // HttpOnly Cookie 방식으로 변경되었으므로, Cookie에서 토큰 추출
        String setCookieHeader = authResult.getResponse().getHeader("Set-Cookie");
        if (setCookieHeader != null && setCookieHeader.contains("accessToken=")) {
            // Cookie에서 accessToken 추출
            String[] cookies = setCookieHeader.split(";");
            for (String cookie : cookies) {
                if (cookie.trim().startsWith("accessToken=")) {
                    accessToken = cookie.trim().substring("accessToken=".length());
                    accessTokenCookie = new Cookie("accessToken", accessToken);
                    break;
                }
            }
        }
        
        // 하위 호환성을 위해 Bearer Token도 사용 가능
        // 테스트 편의를 위해 Bearer Token 방식도 지원

        // 2. 매장 생성
        StoreRequestDto storeRequest = StoreRequestDto.builder()
                .name("스케줄테스트매장")
                .businessType("카페")
                .build();

        MvcResult storeResult = mockMvc.perform(post("/api/v1/stores")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(storeRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        storeId = objectMapper.readTree(storeResult.getResponse().getContentAsString())
                .get("data").get("id").asLong();
    }

    @Test
    @DisplayName("월간 스케줄 조회 API 성공")
    void getMonthlySchedules_success() throws Exception {
        // Given
        Store store = storeRepository.findById(storeId).orElseThrow();

        // 2024년 3월 스케줄 데이터 준비
        // 3월 첫째주 (2/26 ~ 3/3) - 월요일 기준
        Schedule schedule1 = Schedule.builder()
                .weekStartDate(LocalDate.of(2024, 2, 26))
                .status(ScheduleStatus.DRAFT)
                .store(store)
                .build();

        // 3월 둘째주 (3/4 ~ 3/10)
        Schedule schedule2 = Schedule.builder()
                .weekStartDate(LocalDate.of(2024, 3, 4))
                .status(ScheduleStatus.PUBLISHED)
                .store(store)
                .build();

        // 4월 스케줄 (조회되면 안됨)
        Schedule schedule3 = Schedule.builder()
                .weekStartDate(LocalDate.of(2024, 4, 1))
                .status(ScheduleStatus.DRAFT)
                .store(store)
                .build();

        scheduleRepository.save(schedule1);
        scheduleRepository.save(schedule2);
        scheduleRepository.save(schedule3);

        // When & Then
        // 경로를 /api/v1/schedules/monthly로 변경
        // Cookie 방식 또는 Bearer Token 방식 모두 지원 (하위 호환성)
        var requestBuilder = get("/api/v1/schedules/monthly")
                .param("storeId", String.valueOf(storeId))
                .param("yearMonth", "2024-03");
        
        // Cookie가 있으면 Cookie 사용, 없으면 Bearer Token 사용
        if (accessTokenCookie != null) {
            requestBuilder.cookie(accessTokenCookie);
        } else if (accessToken != null) {
            requestBuilder.header("Authorization", "Bearer " + accessToken);
        }
        
        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2))) // 3월에 포함된 2개만 조회
                .andExpect(jsonPath("$[0].weekStartDate").value("2024-02-26"))
                .andExpect(jsonPath("$[0].status").value("DRAFT"))
                .andExpect(jsonPath("$[1].weekStartDate").value("2024-03-04"))
                .andExpect(jsonPath("$[1].status").value("PUBLISHED"));
    }

    @Test
    @DisplayName("타 사용자 스케줄 접근 차단 - 다른 사용자의 storeId로 조회 시 403 반환")
    void getMonthlySchedules_otherUserStore_returns403() throws Exception {
        // Given - 다른 사용자 계정 생성
        SignupRequestDto otherUserSignup = SignupRequestDto.builder()
                .email("otheruser2@example.com")
                .password("Password123!")
                .name("다른사용자2")
                .phone("010-8888-8888")
                .build();

        MvcResult otherUserResult = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(otherUserSignup)))
                .andExpect(status().isCreated())
                .andReturn();

        // HttpOnly Cookie 방식으로 변경되었으므로, Cookie에서 토큰 추출
        String otherUserSetCookieHeader = otherUserResult.getResponse().getHeader("Set-Cookie");
        String otherUserToken = null;
        Cookie otherUserTokenCookie = null;
        if (otherUserSetCookieHeader != null && otherUserSetCookieHeader.contains("accessToken=")) {
            String[] cookies = otherUserSetCookieHeader.split(";");
            for (String cookie : cookies) {
                if (cookie.trim().startsWith("accessToken=")) {
                    otherUserToken = cookie.trim().substring("accessToken=".length());
                    otherUserTokenCookie = new Cookie("accessToken", otherUserToken);
                    break;
                }
            }
        }

        // 다른 사용자의 매장 생성
        StoreRequestDto otherStoreRequest = StoreRequestDto.builder()
                .name("다른사용자매장2")
                .businessType("카페")
                .build();

        var otherStoreRequestBuilder = post("/api/v1/stores")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(otherStoreRequest));
        
        // Cookie 또는 Bearer Token 사용
        if (otherUserTokenCookie != null) {
            otherStoreRequestBuilder.cookie(otherUserTokenCookie);
        } else if (otherUserToken != null) {
            otherStoreRequestBuilder.header("Authorization", "Bearer " + otherUserToken);
        }
        
        MvcResult otherStoreResult = mockMvc.perform(otherStoreRequestBuilder)
                .andExpect(status().isCreated())
                .andReturn();

        Long otherStoreId = objectMapper.readTree(otherStoreResult.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        // When & Then - 다른 사용자의 storeId로 조회 시도 (403 Forbidden)
        // 경로를 /api/v1/schedules/monthly로 변경
        var forbiddenRequestBuilder = get("/api/v1/schedules/monthly")
                .param("storeId", String.valueOf(otherStoreId))
                .param("yearMonth", "2024-03");
        
        // Cookie 또는 Bearer Token 사용
        if (accessTokenCookie != null) {
            forbiddenRequestBuilder.cookie(accessTokenCookie);
        } else if (accessToken != null) {
            forbiddenRequestBuilder.header("Authorization", "Bearer " + accessToken);
        }
        
        mockMvc.perform(forbiddenRequestBuilder)
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("INTG-DASHBOARD-003: 스케줄이 없는 경우 빈 배열 반환 확인")
    void getMonthlySchedules_emptyArray_success() throws Exception {
        // Given - 매장은 생성되어 있지만 스케줄은 없음 (setUp에서 생성된 매장 사용)
        
        // When & Then - 스케줄이 없는 경우 빈 배열 반환 확인 (ApiResponse 래퍼 없음)
        var requestBuilder = get("/api/v1/schedules/monthly")
                .param("storeId", String.valueOf(storeId))
                .param("yearMonth", "2024-12"); // 스케줄이 없는 월
        
        // Cookie 또는 Bearer Token 사용
        if (accessTokenCookie != null) {
            requestBuilder.cookie(accessTokenCookie);
        } else if (accessToken != null) {
            requestBuilder.header("Authorization", "Bearer " + accessToken);
        }
        
        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0))); // 빈 배열 (ApiResponse 래퍼 없음)
    }

    @Test
    @DisplayName("INTG-DASHBOARD-004: Query Parameters 검증 - yearMonth 파라미터 누락 시 400 Bad Request")
    void getMonthlySchedules_missingYearMonth_returns400() throws Exception {
        // Given - 매장은 생성되어 있음 (setUp에서 생성된 매장 사용)
        
        // When & Then - yearMonth 파라미터 누락 시 400 Bad Request
        var requestBuilder = get("/api/v1/schedules/monthly")
                .param("storeId", String.valueOf(storeId));
        // yearMonth 파라미터 누락
        
        // Cookie 또는 Bearer Token 사용
        if (accessTokenCookie != null) {
            requestBuilder.cookie(accessTokenCookie);
        } else if (accessToken != null) {
            requestBuilder.header("Authorization", "Bearer " + accessToken);
        }
        
        mockMvc.perform(requestBuilder)
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("INTG-DASHBOARD-004: Query Parameters 검증 - 잘못된 yearMonth 형식 시 400 Bad Request")
    void getMonthlySchedules_invalidYearMonthFormat_returns400() throws Exception {
        // Given - 매장은 생성되어 있음 (setUp에서 생성된 매장 사용)
        
        // When & Then - 잘못된 yearMonth 형식 시 400 Bad Request
        var requestBuilder = get("/api/v1/schedules/monthly")
                .param("storeId", String.valueOf(storeId))
                .param("yearMonth", "2024/03"); // 잘못된 형식 (yyyy-MM이어야 함)
        
        // Cookie 또는 Bearer Token 사용
        if (accessTokenCookie != null) {
            requestBuilder.cookie(accessTokenCookie);
        } else if (accessToken != null) {
            requestBuilder.header("Authorization", "Bearer " + accessToken);
        }
        
        mockMvc.perform(requestBuilder)
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("INTG-DASHBOARD-004: Query Parameters 검증 - storeId 파라미터 누락 시 400 Bad Request")
    void getMonthlySchedules_missingStoreId_returns400() throws Exception {
        // Given - 매장은 생성되어 있음 (setUp에서 생성된 매장 사용)
        
        // When & Then - storeId 파라미터 누락 시 400 Bad Request
        var requestBuilder = get("/api/v1/schedules/monthly")
                .param("yearMonth", "2024-03");
        // storeId 파라미터 누락
        
        // Cookie 또는 Bearer Token 사용
        if (accessTokenCookie != null) {
            requestBuilder.cookie(accessTokenCookie);
        } else if (accessToken != null) {
            requestBuilder.header("Authorization", "Bearer " + accessToken);
        }
        
        mockMvc.perform(requestBuilder)
                .andExpect(status().isBadRequest());
    }
}

