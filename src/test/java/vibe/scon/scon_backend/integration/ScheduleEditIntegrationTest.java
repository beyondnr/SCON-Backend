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
import vibe.scon.scon_backend.dto.schedule.ShiftRequestDto;
import vibe.scon.scon_backend.dto.schedule.UpdateScheduleRequestDto;
import vibe.scon.scon_backend.dto.store.StoreRequestDto;
import vibe.scon.scon_backend.entity.Employee;
import vibe.scon.scon_backend.entity.Schedule;
import vibe.scon.scon_backend.entity.Shift;
import vibe.scon.scon_backend.entity.Store;
import vibe.scon.scon_backend.entity.enums.EmploymentType;
import vibe.scon.scon_backend.entity.enums.ScheduleStatus;
import vibe.scon.scon_backend.repository.EmployeeRepository;
import vibe.scon.scon_backend.repository.ScheduleRepository;
import vibe.scon.scon_backend.repository.ShiftRepository;
import vibe.scon.scon_backend.repository.StoreRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 스케줄 편집 플로우 통합 테스트.
 * 
 * <p>스케줄 편집 기능에서 필요한 모든 API의 통합 테스트입니다.
 * 스케줄 상세 조회 → 수정 플로우를 검증합니다.</p>
 * 
 * <h3>테스트 케이스 추적:</h3>
 * <ul>
 *   <li>INTG-SCHEDULE-001: 스케줄 상세 조회 → 수정 플로우 성공</li>
 *   <li>INTG-SCHEDULE-002: Cookie 기반 인증 연속 사용 테스트</li>
 *   <li>INTG-SCHEDULE-003: 부분 수정 테스트 (status만, shifts만)</li>
 *   <li>INTG-SCHEDULE-004: 유효성 검증 실패 테스트 (fieldErrors 포함 검증)</li>
 *   <li>INTG-SCHEDULE-005: 에러 케이스 테스트 (존재하지 않는 스케줄, 소유권 없음, PUBLISHED 상태 수정 불가)</li>
 * </ul>
 * 
 * <h3>요구사항 추적 (Traceability):</h3>
 * <ul>
 *   <li>{@code INTG-BE-Phase6-v1.1.0} - 스케줄 편집 기능 (백엔드 작업 계획)</li>
 *   <li>{@code REQ-FUNC-007} - 드래그&드롭 스케줄 편집</li>
 * </ul>
 * 
 * @see <a href="../../../../../../SCON-Docs/BE-FE_Integration/INTG-BE-Phase6-schedule-edit.md">INTG-BE-Phase6-schedule-edit.md</a>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("스케줄 편집 플로우 통합 테스트")
class ScheduleEditIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    private Long storeId;
    private Long employeeId;
    private Long scheduleId;
    private jakarta.servlet.http.Cookie accessTokenCookie;

    /**
     * 공통 설정: 회원가입, 매장 생성, 직원 등록, 스케줄 생성
     */
    private void setUpCommonData() throws Exception {
        // Step 1: 회원가입
        SignupRequestDto signupRequest = SignupRequestDto.builder()
                .email("scheduleedit@example.com")
                .password("Password123!")
                .name("스케줄편집테스터")
                .phone("010-3333-4444")
                .build();

        MvcResult signupResult = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        // Cookie에서 accessToken 추출
        jakarta.servlet.http.Cookie[] cookies = signupResult.getResponse().getCookies();
        if (cookies != null) {
            for (jakarta.servlet.http.Cookie cookie : cookies) {
                if ("accessToken".equals(cookie.getName())) {
                    accessTokenCookie = cookie;
                    break;
                }
            }
        }

        assertThat(accessTokenCookie).isNotNull();

        // Step 2: 매장 생성
        StoreRequestDto storeRequest = StoreRequestDto.builder()
                .name("스케줄편집테스트매장")
                .businessType("베이커리카페")
                .build();

        MvcResult storeResult = mockMvc.perform(post("/api/v1/stores")
                        .cookie(accessTokenCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(storeRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        storeId = objectMapper.readTree(storeResult.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        // Step 3: 직원 등록
        EmployeeRequestDto employeeRequest = EmployeeRequestDto.builder()
                .name("테스트직원")
                .phone("010-5555-6666")
                .hourlyWage(new BigDecimal("10000"))
                .employmentType(EmploymentType.EMPLOYEE)
                .build();

        MvcResult employeeResult = mockMvc.perform(post("/api/v1/stores/{storeId}/employees", storeId)
                        .cookie(accessTokenCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(employeeRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        employeeId = objectMapper.readTree(employeeResult.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        // Step 4: 스케줄 생성 (DRAFT 상태)
        Store store = storeRepository.findById(storeId).orElseThrow();
        Employee employee = employeeRepository.findById(employeeId).orElseThrow();
        
        LocalDate weekStartDate = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
        Schedule schedule = Schedule.builder()
                .weekStartDate(weekStartDate)
                .status(ScheduleStatus.DRAFT)
                .store(store)
                .build();
        
        schedule = scheduleRepository.save(schedule);

        // Shift 생성
        Shift shift = Shift.builder()
                .workDate(weekStartDate)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(18, 0))
                .schedule(schedule)
                .employee(employee)
                .build();
        
        schedule.addShift(shift);
        schedule = scheduleRepository.save(schedule);
        scheduleId = schedule.getId();
    }

    @Test
    @DisplayName("INTG-SCHEDULE-001: 스케줄 상세 조회 → 수정 플로우 성공")
    void scheduleEdit_getScheduleDetailAndUpdateFlow_success() throws Exception {
        // Given
        setUpCommonData();

        // Step 1: 스케줄 상세 조회
        mockMvc.perform(get("/api/v1/schedules/{id}", scheduleId)
                        .cookie(accessTokenCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("스케줄 조회 성공"))
                .andExpect(jsonPath("$.data.id").value(scheduleId.intValue()))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.shifts", hasSize(1)))
                .andExpect(jsonPath("$.data.shifts[0].employeeId").value(employeeId.intValue()));

        // Step 2: 스케줄 수정 (status와 shifts 모두 업데이트)
        List<ShiftRequestDto> newShifts = new ArrayList<>();
        newShifts.add(ShiftRequestDto.builder()
                .employeeId(employeeId)
                .workDate(LocalDate.now().with(java.time.DayOfWeek.MONDAY))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(19, 0))
                .build());

        UpdateScheduleRequestDto updateRequest = UpdateScheduleRequestDto.builder()
                .status(ScheduleStatus.PENDING)
                .shifts(newShifts)
                .build();

        mockMvc.perform(put("/api/v1/schedules/{id}", scheduleId)
                        .cookie(accessTokenCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("스케줄이 수정되었습니다"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.shifts", hasSize(1)))
                .andExpect(jsonPath("$.data.shifts[0].startTime").value("10:00:00"))
                .andExpect(jsonPath("$.data.shifts[0].endTime").value("19:00:00"));

        // Step 3: 수정 후 조회하여 변경사항 확인
        mockMvc.perform(get("/api/v1/schedules/{id}", scheduleId)
                        .cookie(accessTokenCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.shifts[0].startTime").value("10:00:00"));
    }

    @Test
    @DisplayName("INTG-SCHEDULE-002: Cookie 기반 인증 연속 사용 테스트")
    void scheduleEdit_cookieBasedAuthContinuousUsage_success() throws Exception {
        // Given
        setUpCommonData();

        // 조회 → 수정 플로우에서 HttpOnly Cookie 자동 사용 확인
        mockMvc.perform(get("/api/v1/schedules/{id}", scheduleId)
                        .cookie(accessTokenCookie))
                .andExpect(status().isOk());

        List<ShiftRequestDto> newShifts = new ArrayList<>();
        newShifts.add(ShiftRequestDto.builder()
                .employeeId(employeeId)
                .workDate(LocalDate.now().with(java.time.DayOfWeek.MONDAY))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(19, 0))
                .build());

        UpdateScheduleRequestDto updateRequest = UpdateScheduleRequestDto.builder()
                .status(ScheduleStatus.PENDING)
                .shifts(newShifts)
                .build();

        mockMvc.perform(put("/api/v1/schedules/{id}", scheduleId)
                        .cookie(accessTokenCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("INTG-SCHEDULE-003: 부분 수정 테스트 (status만, shifts만)")
    void scheduleEdit_partialUpdate_success() throws Exception {
        // Given
        setUpCommonData();

        // Step 1: status만 수정 (shifts는 null)
        UpdateScheduleRequestDto statusOnlyRequest = UpdateScheduleRequestDto.builder()
                .status(ScheduleStatus.PENDING)
                .shifts(null)
                .build();

        mockMvc.perform(put("/api/v1/schedules/{id}", scheduleId)
                        .cookie(accessTokenCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusOnlyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.shifts", hasSize(1))); // 기존 shifts 유지

        // Step 2: shifts만 수정 (status는 null)
        List<ShiftRequestDto> newShifts = new ArrayList<>();
        newShifts.add(ShiftRequestDto.builder()
                .employeeId(employeeId)
                .workDate(LocalDate.now().with(java.time.DayOfWeek.MONDAY))
                .startTime(LocalTime.of(11, 0))
                .endTime(LocalTime.of(20, 0))
                .build());

        UpdateScheduleRequestDto shiftsOnlyRequest = UpdateScheduleRequestDto.builder()
                .status(null)
                .shifts(newShifts)
                .build();

        mockMvc.perform(put("/api/v1/schedules/{id}", scheduleId)
                        .cookie(accessTokenCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(shiftsOnlyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING")) // 기존 status 유지
                .andExpect(jsonPath("$.data.shifts[0].startTime").value("11:00:00"));
    }

    @Test
    @DisplayName("INTG-SCHEDULE-004: 유효성 검증 실패 테스트 (fieldErrors 포함 검증)")
    void scheduleEdit_validationFailure_returnsFieldErrors() throws Exception {
        // Given
        setUpCommonData();

        // 시작 시간 >= 종료 시간
        List<ShiftRequestDto> invalidShifts = new ArrayList<>();
        invalidShifts.add(ShiftRequestDto.builder()
                .employeeId(employeeId)
                .workDate(LocalDate.now().with(java.time.DayOfWeek.MONDAY))
                .startTime(LocalTime.of(18, 0)) // 종료 시간보다 늦음
                .endTime(LocalTime.of(9, 0))
                .build());

        UpdateScheduleRequestDto invalidRequest = UpdateScheduleRequestDto.builder()
                .status(ScheduleStatus.DRAFT)
                .shifts(invalidShifts)
                .build();

        mockMvc.perform(put("/api/v1/schedules/{id}", scheduleId)
                        .cookie(accessTokenCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value(containsString("시작 시간")));

        // 필수 필드 누락 (employeeId가 null)
        List<ShiftRequestDto> missingFieldShifts = new ArrayList<>();
        missingFieldShifts.add(ShiftRequestDto.builder()
                .employeeId(null) // 필수 필드 누락
                .workDate(LocalDate.now().with(java.time.DayOfWeek.MONDAY))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(18, 0))
                .build());

        UpdateScheduleRequestDto missingFieldRequest = UpdateScheduleRequestDto.builder()
                .status(ScheduleStatus.DRAFT)
                .shifts(missingFieldShifts)
                .build();

        mockMvc.perform(put("/api/v1/schedules/{id}", scheduleId)
                        .cookie(accessTokenCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(missingFieldRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'shifts[0].employeeId')]").exists());
    }

    @Test
    @DisplayName("INTG-SCHEDULE-005: 에러 케이스 테스트 (존재하지 않는 스케줄, 소유권 없음, PUBLISHED 상태 수정 불가)")
    void scheduleEdit_errorCases_returnsAppropriateErrors() throws Exception {
        // Given
        setUpCommonData();

        // Case 1: 존재하지 않는 스케줄 조회
        mockMvc.perform(get("/api/v1/schedules/{id}", 99999L)
                        .cookie(accessTokenCookie))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(containsString("스케줄을 찾을 수 없습니다")));

        // Case 2: 존재하지 않는 스케줄 수정
        UpdateScheduleRequestDto updateRequest = UpdateScheduleRequestDto.builder()
                .status(ScheduleStatus.PENDING)
                .shifts(new ArrayList<>())
                .build();

        mockMvc.perform(put("/api/v1/schedules/{id}", 99999L)
                        .cookie(accessTokenCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound());

        // Case 3: PUBLISHED 상태 스케줄 수정 불가
        Store store = storeRepository.findById(storeId).orElseThrow();
        LocalDate weekStartDate = LocalDate.now().with(java.time.DayOfWeek.MONDAY).plusWeeks(1);
        Schedule publishedSchedule = Schedule.builder()
                .weekStartDate(weekStartDate)
                .status(ScheduleStatus.PUBLISHED)
                .store(store)
                .build();
        publishedSchedule = scheduleRepository.save(publishedSchedule);

        UpdateScheduleRequestDto updatePublishedRequest = UpdateScheduleRequestDto.builder()
                .status(ScheduleStatus.DRAFT)
                .shifts(new ArrayList<>())
                .build();

        mockMvc.perform(put("/api/v1/schedules/{id}", publishedSchedule.getId())
                        .cookie(accessTokenCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePublishedRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("수정할 수 없습니다")));

        // Case 4: 다른 사용자의 스케줄 접근 (소유권 없음)
        // 다른 사용자 계정 생성
        SignupRequestDto otherUserSignup = SignupRequestDto.builder()
                .email("otheruser@example.com")
                .password("Password123!")
                .name("다른사용자")
                .phone("010-7777-8888")
                .build();

        MvcResult otherUserResult = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(otherUserSignup)))
                .andExpect(status().isCreated())
                .andReturn();

        jakarta.servlet.http.Cookie[] otherCookies = otherUserResult.getResponse().getCookies();
        jakarta.servlet.http.Cookie otherUserCookie = null;
        if (otherCookies != null) {
            for (jakarta.servlet.http.Cookie cookie : otherCookies) {
                if ("accessToken".equals(cookie.getName())) {
                    otherUserCookie = cookie;
                    break;
                }
            }
        }

        // 다른 사용자의 스케줄 조회 시도
        mockMvc.perform(get("/api/v1/schedules/{id}", scheduleId)
                        .cookie(otherUserCookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value(containsString("접근 권한이 없습니다")));
    }
}
