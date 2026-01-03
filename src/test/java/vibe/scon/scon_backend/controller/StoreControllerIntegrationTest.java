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

        String responseBody = result.getResponse().getContentAsString();
        accessToken = objectMapper.readTree(responseBody).get("data").get("accessToken").asText();
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

        String otherAccessToken = objectMapper.readTree(otherResult.getResponse().getContentAsString())
                .get("data").get("accessToken").asText();

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
