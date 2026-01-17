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
import vibe.scon.scon_backend.entity.enums.ScheduleStatus;
import vibe.scon.scon_backend.repository.ScheduleRepository;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * StoreController 통합 테스트.
 * 
 * <h3>테스트 케이스 추적:</h3>
 * <ul>
 *   <li>TC-STORE-001: 매장 생성 API</li>
 *   <li>TC-STORE-002: 매장 조회 API</li>
 *   <li>TC-STORE-004: 매장 수정 API</li>
 *   <li>TC-STORE-005: 내 매장 목록 조회 API</li>
 *   <li>TC-STORE-006: 타 사용자 매장 접근 차단 (403)</li>
 *   <li>TC-STORE-007: 존재하지 않는 매장 조회 (404)</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("StoreController 통합 테스트")
class StoreControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ScheduleRepository scheduleRepository;

    private String accessToken;
    private Long ownerId;

    @BeforeEach
    void setUp() throws Exception {
        // 회원가입하여 토큰 획득
        SignupRequestDto signupRequest = SignupRequestDto.builder()
                .email("storetest@example.com")
                .password("Password123!")
                .name("매장테스트")
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
    @DisplayName("TC-STORE-001: 매장 생성 API 성공")
    void createStore_success() throws Exception {
        // Given
        StoreRequestDto storeRequest = StoreRequestDto.builder()
                .name("테스트베이커리")
                .businessType("베이커리카페")
                .address("서울시 강남구 테헤란로 123")
                .openTime(LocalTime.of(8, 0))
                .closeTime(LocalTime.of(22, 0))
                .storeHoliday(DayOfWeek.MONDAY) // 휴무일 추가
                .build();

        // When & Then - 매장 생성은 201 Created 반환
        MvcResult result = mockMvc.perform(post("/api/v1/stores")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(storeRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.name").value("테스트베이커리"))
                .andExpect(jsonPath("$.data.businessType").value("베이커리카페"))
                .andExpect(jsonPath("$.data.storeHoliday").value("MONDAY")) // 휴무일 검증
                .andReturn();

        // Draft 스케줄 자동 생성 검증 (UX 문서 요구사항)
        Long storeId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data").get("id").asLong();
        List<Schedule> draftSchedules = scheduleRepository.findByStoreIdAndStatus(storeId, ScheduleStatus.DRAFT);
        assertThat(draftSchedules).hasSize(1);
        assertThat(draftSchedules.get(0).getWeekStartDate()).isNotNull();
        assertThat(draftSchedules.get(0).getStatus()).isEqualTo(ScheduleStatus.DRAFT);
    }

    @Test
    @DisplayName("TC-STORE-002: 매장 조회 API 성공")
    void getStore_success() throws Exception {
        // Given - 매장 생성
        StoreRequestDto storeRequest = StoreRequestDto.builder()
                .name("조회테스트매장")
                .businessType("카페")
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/stores")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(storeRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        Long storeId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        // When & Then
        mockMvc.perform(get("/api/v1/stores/{id}", storeId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(storeId))
                .andExpect(jsonPath("$.data.name").value("조회테스트매장"));
    }

    @Test
    @DisplayName("TC-STORE-004: 매장 수정 API 성공")
    void updateStore_success() throws Exception {
        // Given - 매장 생성
        StoreRequestDto createRequest = StoreRequestDto.builder()
                .name("원래이름")
                .businessType("카페")
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/stores")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        Long storeId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        // When & Then - 수정
        StoreRequestDto updateRequest = StoreRequestDto.builder()
                .name("변경된이름")
                .businessType("베이커리")
                .address("새주소")
                .storeHoliday(DayOfWeek.SUNDAY) // 휴무일 수정
                .build();

        mockMvc.perform(put("/api/v1/stores/{id}", storeId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("변경된이름"))
                .andExpect(jsonPath("$.data.businessType").value("베이커리"))
                .andExpect(jsonPath("$.data.address").value("새주소"))
                .andExpect(jsonPath("$.data.storeHoliday").value("SUNDAY")); // 수정된 휴무일 검증
    }

    @Test
    @DisplayName("INTG-SETTINGS-003: 매장 정보 부분 수정 테스트")
    void updateStore_partialUpdate_success() throws Exception {
        // Given - 매장 생성 (모든 필드 포함)
        StoreRequestDto createRequest = StoreRequestDto.builder()
                .name("부분수정테스트매장")
                .businessType("베이커리")
                .address("서울시 강남구")
                .openTime(LocalTime.of(9, 0))
                .closeTime(LocalTime.of(18, 0))
                .storeHoliday(DayOfWeek.SATURDAY)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/stores")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        Long storeId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        // When & Then - 부분 수정 (이름과 주소만 수정, 나머지는 기존 값 유지)
        // name은 @NotBlank로 필수이므로 포함해야 함
        // Store.update() 메서드가 null 체크를 통해 부분 업데이트를 지원
        StoreRequestDto partialUpdateRequest = StoreRequestDto.builder()
                .name("부분수정된매장명") // 이름 수정 (필수)
                .businessType(null) // 업종은 수정하지 않음 (null 체크로 인해 업데이트 안 됨)
                .address("서울시 강남구 테헤란로 456") // 주소만 수정
                .openTime(null) // 영업시간은 수정하지 않음
                .closeTime(null)
                .storeHoliday(DayOfWeek.SATURDAY) // 휴무일은 기존 값 유지 (null로 전달하면 null로 업데이트되므로)
                .build();

        mockMvc.perform(put("/api/v1/stores/{id}", storeId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(partialUpdateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("부분수정된매장명"))
                .andExpect(jsonPath("$.data.address").value("서울시 강남구 테헤란로 456"))
                .andExpect(jsonPath("$.data.businessType").value("베이커리")) // 변경되지 않음 (null 체크)
                .andExpect(jsonPath("$.data.openTime").value("09:00:00")) // 변경되지 않음 (null 체크)
                .andExpect(jsonPath("$.data.closeTime").value("18:00:00")) // 변경되지 않음 (null 체크)
                .andExpect(jsonPath("$.data.storeHoliday").value("SATURDAY")); // 기존 값 유지
    }

    @Test
    @DisplayName("INTG-SETTINGS-006: 매장 정보 수정 시 유효성 검증 실패 테스트 (fieldErrors 포함)")
    void updateStore_validationFailure_returnsFieldErrors() throws Exception {
        // Given - 매장 생성
        StoreRequestDto createRequest = StoreRequestDto.builder()
                .name("유효성검증매장")
                .businessType("카페")
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/stores")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        Long storeId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        // When & Then - 유효성 검증 실패 (name이 빈 문자열)
        StoreRequestDto invalidRequest = StoreRequestDto.builder()
                .name("") // @NotBlank 검증 실패
                .businessType("카페")
                .build();

        mockMvc.perform(put("/api/v1/stores/{id}", storeId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.fieldErrors[0].field").exists())
                .andExpect(jsonPath("$.fieldErrors[0].message").exists())
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("name"))); // name 필드 에러 포함
    }

    @Test
    @DisplayName("TC-STORE-005: 내 매장 목록 조회 API 성공")
    void getMyStores_success() throws Exception {
        // Given - 매장 2개 생성
        StoreRequestDto store1 = StoreRequestDto.builder().name("매장1").businessType("카페").build();
        StoreRequestDto store2 = StoreRequestDto.builder().name("매장2").businessType("베이커리").build();

        mockMvc.perform(post("/api/v1/stores")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(store1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/stores")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(store2)))
                .andExpect(status().isCreated());

        // When & Then
        mockMvc.perform(get("/api/v1/stores")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    @DisplayName("TC-STORE-007: 존재하지 않는 매장 조회 시 404")
    void getStore_notFound_returns404() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/stores/{id}", 99999L)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("인증 없이 매장 조회 시 401")
    void getStores_withoutAuth_returns401() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/stores"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("TC-STORE-006: 타 사용자 매장 접근 차단")
    void getStore_otherUserStore_returns403() throws Exception {
        // Given - 첫 번째 사용자로 매장 생성
        StoreRequestDto storeRequest = StoreRequestDto.builder()
                .name("다른사람매장")
                .businessType("카페")
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/stores")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(storeRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        Long storeId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        // 두 번째 사용자 생성
        SignupRequestDto otherUser = SignupRequestDto.builder()
                .email("otheruser@example.com")
                .password("Password123!")
                .name("다른사용자")
                .build();

        MvcResult otherResult = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(otherUser)))
                .andExpect(status().isCreated())
                .andReturn();

        // Cookie에서 accessToken 추출
        String otherAccessToken = null;
        jakarta.servlet.http.Cookie[] otherUserCookies = otherResult.getResponse().getCookies();
        if (otherUserCookies != null) {
            for (jakarta.servlet.http.Cookie cookie : otherUserCookies) {
                if ("accessToken".equals(cookie.getName())) {
                    otherAccessToken = cookie.getValue();
                    break;
                }
            }
        }
        
        // Cookie에서 토큰을 찾지 못한 경우 응답 본문에서 추출 시도 (하위 호환성)
        if (otherAccessToken == null) {
            try {
                otherAccessToken = objectMapper.readTree(otherResult.getResponse().getContentAsString())
                        .get("data").get("accessToken").asText();
            } catch (Exception e) {
                // 토큰이 응답 본문에 없는 경우 (Cookie 방식)
                otherAccessToken = null;
            }
        }

        // When & Then - 다른 사용자가 접근 시도
        mockMvc.perform(get("/api/v1/stores/{id}", storeId)
                        .header("Authorization", "Bearer " + otherAccessToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("매장 생성 시 Draft 스케줄 자동 생성 (UX 플로우)")
    void createStore_autoCreatesDraftSchedule() throws Exception {
        // Given
        StoreRequestDto storeRequest = StoreRequestDto.builder()
                .name("온보딩테스트매장")
                .businessType("카페")
                .build();

        // When - 매장 생성
        MvcResult result = mockMvc.perform(post("/api/v1/stores")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(storeRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        Long storeId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        // Then - Draft 스케줄이 자동 생성되었는지 확인
        List<Schedule> draftSchedules = scheduleRepository.findByStoreIdAndStatus(storeId, ScheduleStatus.DRAFT);
        assertThat(draftSchedules).hasSize(1);
        
        Schedule draftSchedule = draftSchedules.get(0);
        assertThat(draftSchedule.getStore().getId()).isEqualTo(storeId);
        assertThat(draftSchedule.getStatus()).isEqualTo(ScheduleStatus.DRAFT);
        assertThat(draftSchedule.getWeekStartDate()).isNotNull();
    }
}
