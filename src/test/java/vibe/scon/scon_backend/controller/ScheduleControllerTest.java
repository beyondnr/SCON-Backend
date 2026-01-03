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

        accessToken = objectMapper.readTree(authResult.getResponse().getContentAsString())
                .get("data").get("accessToken").asText();

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
        mockMvc.perform(get("/api/schedules/monthly")
                        .param("storeId", String.valueOf(storeId))
                        .param("yearMonth", "2024-03")
                        .header("Authorization", "Bearer " + accessToken))
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

        String otherUserToken = objectMapper.readTree(otherUserResult.getResponse().getContentAsString())
                .get("data").get("accessToken").asText();

        // 다른 사용자의 매장 생성
        StoreRequestDto otherStoreRequest = StoreRequestDto.builder()
                .name("다른사용자매장2")
                .businessType("카페")
                .build();

        MvcResult otherStoreResult = mockMvc.perform(post("/api/v1/stores")
                        .header("Authorization", "Bearer " + otherUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(otherStoreRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        Long otherStoreId = objectMapper.readTree(otherStoreResult.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        // When & Then - 다른 사용자의 storeId로 조회 시도 (403 Forbidden)
        mockMvc.perform(get("/api/schedules/monthly")
                        .param("storeId", String.valueOf(otherStoreId))
                        .param("yearMonth", "2024-03")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden());
    }
}

