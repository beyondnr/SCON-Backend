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
import vibe.scon.scon_backend.dto.employee.EmployeeRequestDto;
import vibe.scon.scon_backend.dto.store.StoreRequestDto;
import vibe.scon.scon_backend.entity.Schedule;
import vibe.scon.scon_backend.entity.Store;
import vibe.scon.scon_backend.entity.enums.EmploymentType;
import vibe.scon.scon_backend.entity.enums.ScheduleStatus;
import vibe.scon.scon_backend.repository.ScheduleRepository;
import vibe.scon.scon_backend.repository.StoreRepository;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 대시보드 데이터 로드 통합 테스트.
 * 
 * <p>대시보드 페이지에서 필요한 모든 데이터를 한 번에 로드하는 플로우를 검증합니다.
 * 매장 정보, 직원 목록, 월간 스케줄을 순차적으로 조회하는 통합 테스트입니다.</p>
 * 
 * <h3>테스트 케이스 추적:</h3>
 * <ul>
 *   <li>INTG-DASHBOARD-001: 대시보드 데이터 전체 로드 성공</li>
 *   <li>INTG-DASHBOARD-002: Cookie 기반 인증 연속 사용 테스트</li>
 *   <li>INTG-DASHBOARD-003: 빈 배열 반환 테스트 (직원 없음, 스케줄 없음)</li>
 *   <li>INTG-DASHBOARD-004: Query Parameters 검증 테스트 (yearMonth 형식 오류)</li>
 * </ul>
 * 
 * <h3>요구사항 추적 (Traceability):</h3>
 * <ul>
 *   <li>{@code INTG-BE-Phase3-v1.1.0} - 대시보드 연동 (백엔드 작업 계획)</li>
 *   <li>{@code REQ-FUNC-004} - 대시보드 데이터 로드</li>
 * </ul>
 * 
 * @see <a href="../../../../../../SCON-Docs/BE-FE_Integration/INTG-BE-Phase3-dashboard.md">INTG-BE-Phase3-dashboard.md</a>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("대시보드 데이터 로드 통합 테스트")
class DashboardIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Test
    @DisplayName("INTG-DASHBOARD-001: 대시보드 데이터 전체 로드 성공")
    void dashboard_loadAllData_success() throws Exception {
        // Step 1: 회원가입 및 매장 생성
        SignupRequestDto signupRequest = SignupRequestDto.builder()
                .email("dashboard@example.com")
                .password("Password123!")
                .name("대시보드테스트사장")
                .phone("010-1111-2222")
                .build();

        MvcResult signupResult = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        // Cookie에서 accessToken 추출
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

        // 매장 생성
        StoreRequestDto storeRequest = StoreRequestDto.builder()
                .name("대시보드테스트매장")
                .businessType("베이커리카페")
                .address("서울시 강남구 테헤란로 123")
                .build();

        MvcResult storeResult = mockMvc.perform(post("/api/v1/stores")
                        .cookie(accessTokenCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(storeRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        Long storeId = objectMapper.readTree(storeResult.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        // 직원 등록
        EmployeeRequestDto employeeRequest = EmployeeRequestDto.builder()
                .name("김직원")
                .phone("010-3333-4444")
                .hourlyWage(new BigDecimal("9860"))
                .employmentType(EmploymentType.EMPLOYEE)
                .build();

        mockMvc.perform(post("/api/v1/stores/{storeId}/employees", storeId)
                        .cookie(accessTokenCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(employeeRequest)))
                .andExpect(status().isCreated());

        // 스케줄 데이터 준비 (직접 Repository 사용)
        Store store = storeRepository.findById(storeId).orElseThrow();
        Schedule schedule = Schedule.builder()
                .weekStartDate(LocalDate.of(2024, 3, 4))
                .status(ScheduleStatus.DRAFT)
                .store(store)
                .build();
        scheduleRepository.save(schedule);

        // Step 2: 대시보드 데이터 로드 (매장 정보 + 직원 목록 + 스케줄)
        
        // 2-1. 매장 정보 조회
        mockMvc.perform(get("/api/v1/stores/{id}", storeId)
                        .cookie(accessTokenCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("매장 조회 성공"))
                .andExpect(jsonPath("$.data.id").value(storeId.intValue()))
                .andExpect(jsonPath("$.data.name").value("대시보드테스트매장"));

        // 2-2. 직원 목록 조회
        mockMvc.perform(get("/api/v1/stores/{storeId}/employees", storeId)
                        .cookie(accessTokenCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("직원 목록 조회 성공"))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("김직원"))
                .andExpect(jsonPath("$.data[0].phone").value("010-3333-4444"));

        // 2-3. 월간 스케줄 조회 (2024년 3월)
        mockMvc.perform(get("/api/v1/schedules/monthly")
                        .cookie(accessTokenCookie)
                        .param("storeId", String.valueOf(storeId))
                        .param("yearMonth", "2024-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].weekStartDate").value("2024-03-04"))
                .andExpect(jsonPath("$[0].status").value("DRAFT"))
                .andExpect(jsonPath("$[0].storeId").value(storeId.intValue()));
    }

    @Test
    @DisplayName("INTG-DASHBOARD-002: Cookie 기반 인증 연속 사용 테스트")
    void dashboard_cookieContinuousUsage_success() throws Exception {
        // Step 1: 회원가입 및 매장 생성
        SignupRequestDto signupRequest = SignupRequestDto.builder()
                .email("cookiedash@example.com")
                .password("Password123!")
                .name("쿠키대시보드테스트")
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

        assertThat(accessTokenCookie).isNotNull();

        // 매장 생성
        StoreRequestDto storeRequest = StoreRequestDto.builder()
                .name("쿠키대시보드매장")
                .businessType("카페")
                .build();

        MvcResult storeResult = mockMvc.perform(post("/api/v1/stores")
                        .cookie(accessTokenCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(storeRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        Long storeId = objectMapper.readTree(storeResult.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        // Step 2: Cookie를 연속으로 사용하여 대시보드 데이터 로드
        
        // 매장 정보 조회 (Cookie 사용)
        mockMvc.perform(get("/api/v1/stores/{id}", storeId)
                        .cookie(accessTokenCookie))
                .andExpect(status().isOk());

        // 직원 목록 조회 (같은 Cookie 사용)
        mockMvc.perform(get("/api/v1/stores/{storeId}/employees", storeId)
                        .cookie(accessTokenCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", isA(java.util.List.class)));

        // 월간 스케줄 조회 (같은 Cookie 사용)
        mockMvc.perform(get("/api/v1/schedules/monthly")
                        .cookie(accessTokenCookie)
                        .param("storeId", String.valueOf(storeId))
                        .param("yearMonth", "2024-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(java.util.List.class)));
    }

    @Test
    @DisplayName("INTG-DASHBOARD-003: 빈 배열 반환 테스트 - 직원 없음, 스케줄 없음")
    void dashboard_emptyArrayResponse_success() throws Exception {
        // Step 1: 회원가입 및 매장 생성
        SignupRequestDto signupRequest = SignupRequestDto.builder()
                .email("emptyarray@example.com")
                .password("Password123!")
                .name("빈배열테스트")
                .phone("010-7777-8888")
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

        // 매장 생성
        StoreRequestDto storeRequest = StoreRequestDto.builder()
                .name("빈배열테스트매장")
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

        // Step 2: 직원이 없는 경우 빈 배열 반환 확인
        mockMvc.perform(get("/api/v1/stores/{storeId}/employees", storeId)
                        .cookie(accessTokenCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("직원 목록 조회 성공"))
                .andExpect(jsonPath("$.data", hasSize(0))); // 빈 배열

        // Step 3: 스케줄이 없는 경우 빈 배열 반환 확인
        mockMvc.perform(get("/api/v1/schedules/monthly")
                        .cookie(accessTokenCookie)
                        .param("storeId", String.valueOf(storeId))
                        .param("yearMonth", "2024-12")) // 스케줄이 없는 월
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0))); // 빈 배열 (ApiResponse 래퍼 없음)
    }

    @Test
    @DisplayName("INTG-DASHBOARD-004: Query Parameters 검증 테스트 - yearMonth 형식 오류")
    void dashboard_queryParameterValidation_fails() throws Exception {
        // Step 1: 회원가입 및 매장 생성
        SignupRequestDto signupRequest = SignupRequestDto.builder()
                .email("queryvalidation@example.com")
                .password("Password123!")
                .name("쿼리검증테스트")
                .phone("010-9999-0000")
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

        // 매장 생성
        StoreRequestDto storeRequest = StoreRequestDto.builder()
                .name("쿼리검증테스트매장")
                .businessType("카페")
                .build();

        MvcResult storeResult = mockMvc.perform(post("/api/v1/stores")
                        .cookie(accessTokenCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(storeRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        Long storeId = objectMapper.readTree(storeResult.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        // Step 2: yearMonth 파라미터 누락 시 400 Bad Request
        mockMvc.perform(get("/api/v1/schedules/monthly")
                        .cookie(accessTokenCookie)
                        .param("storeId", String.valueOf(storeId)))
                .andExpect(status().isBadRequest());

        // Step 3: 잘못된 yearMonth 형식 시 400 Bad Request
        mockMvc.perform(get("/api/v1/schedules/monthly")
                        .cookie(accessTokenCookie)
                        .param("storeId", String.valueOf(storeId))
                        .param("yearMonth", "2024/03")) // 잘못된 형식 (yyyy-MM이어야 함)
                .andExpect(status().isBadRequest());

        // Step 4: 유효하지 않은 날짜 형식 시 400 Bad Request
        mockMvc.perform(get("/api/v1/schedules/monthly")
                        .cookie(accessTokenCookie)
                        .param("storeId", String.valueOf(storeId))
                        .param("yearMonth", "2024-13")) // 존재하지 않는 월
                .andExpect(status().isBadRequest());

        // Step 5: storeId 파라미터 누락 시 400 Bad Request
        mockMvc.perform(get("/api/v1/schedules/monthly")
                        .cookie(accessTokenCookie)
                        .param("yearMonth", "2024-03"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("대시보드 데이터 로드 - 매장 정보 + 직원 목록 + 스케줄 순차 조회 성공")
    void dashboard_sequentialDataLoad_success() throws Exception {
        // Step 1: 회원가입 및 매장 생성
        SignupRequestDto signupRequest = SignupRequestDto.builder()
                .email("sequential@example.com")
                .password("Password123!")
                .name("순차로드테스트")
                .phone("010-1212-3434")
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

        // 매장 생성
        StoreRequestDto storeRequest = StoreRequestDto.builder()
                .name("순차로드테스트매장")
                .businessType("베이커리카페")
                .build();

        MvcResult storeResult = mockMvc.perform(post("/api/v1/stores")
                        .cookie(accessTokenCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(storeRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        Long storeId = objectMapper.readTree(storeResult.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        // 직원 2명 등록
        EmployeeRequestDto employee1 = EmployeeRequestDto.builder()
                .name("직원1")
                .phone("010-1111-1111")
                .hourlyWage(new BigDecimal("9860"))
                .employmentType(EmploymentType.EMPLOYEE)
                .build();

        EmployeeRequestDto employee2 = EmployeeRequestDto.builder()
                .name("직원2")
                .phone("010-2222-2222")
                .hourlyWage(new BigDecimal("12000"))
                .employmentType(EmploymentType.MANAGER)
                .build();

        mockMvc.perform(post("/api/v1/stores/{storeId}/employees", storeId)
                        .cookie(accessTokenCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(employee1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/stores/{storeId}/employees", storeId)
                        .cookie(accessTokenCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(employee2)))
                .andExpect(status().isCreated());

        // 스케줄 데이터 준비
        Store store = storeRepository.findById(storeId).orElseThrow();
        Schedule schedule1 = Schedule.builder()
                .weekStartDate(LocalDate.of(2024, 3, 4))
                .status(ScheduleStatus.DRAFT)
                .store(store)
                .build();
        Schedule schedule2 = Schedule.builder()
                .weekStartDate(LocalDate.of(2024, 3, 11))
                .status(ScheduleStatus.PUBLISHED)
                .store(store)
                .build();
        scheduleRepository.save(schedule1);
        scheduleRepository.save(schedule2);

        // Step 2: 대시보드 데이터 순차 로드
        
        // 1) 매장 정보 조회
        MvcResult storeInfoResult = mockMvc.perform(get("/api/v1/stores/{id}", storeId)
                        .cookie(accessTokenCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("순차로드테스트매장"))
                .andReturn();

        // 2) 직원 목록 조회
        MvcResult employeesResult = mockMvc.perform(get("/api/v1/stores/{storeId}/employees", storeId)
                        .cookie(accessTokenCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].name").exists())
                .andExpect(jsonPath("$.data[1].name").exists())
                .andReturn();

        // 3) 월간 스케줄 조회
        MvcResult schedulesResult = mockMvc.perform(get("/api/v1/schedules/monthly")
                        .cookie(accessTokenCookie)
                        .param("storeId", String.valueOf(storeId))
                        .param("yearMonth", "2024-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].weekStartDate").value("2024-03-04"))
                .andExpect(jsonPath("$[1].weekStartDate").value("2024-03-11"))
                .andReturn();

        // 모든 데이터가 정상적으로 로드되었는지 확인
        assertThat(storeInfoResult.getResponse().getStatus()).isEqualTo(200);
        assertThat(employeesResult.getResponse().getStatus()).isEqualTo(200);
        assertThat(schedulesResult.getResponse().getStatus()).isEqualTo(200);
    }
}
