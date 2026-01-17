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
import vibe.scon.scon_backend.entity.Employee;
import vibe.scon.scon_backend.entity.Schedule;
import vibe.scon.scon_backend.entity.Shift;
import vibe.scon.scon_backend.entity.Store;
import vibe.scon.scon_backend.entity.enums.EmploymentType;
import vibe.scon.scon_backend.entity.enums.ScheduleStatus;
import vibe.scon.scon_backend.entity.enums.ShiftPreset;
import vibe.scon.scon_backend.repository.EmployeeRepository;
import vibe.scon.scon_backend.repository.ScheduleRepository;
import vibe.scon.scon_backend.repository.ShiftRepository;
import vibe.scon.scon_backend.repository.StoreRepository;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 설정 페이지 플로우 통합 테스트.
 * 
 * <p>설정 페이지(직원 관리 및 매장 정보 관리)에서 필요한 모든 API의 통합 테스트입니다.
 * 매장 정보 조회 → 수정, 직원 목록 조회 → 등록 → 수정 → 삭제 플로우를 검증합니다.</p>
 * 
 * <h3>테스트 케이스 추적:</h3>
 * <ul>
 *   <li>INTG-SETTINGS-001: 매장 정보 조회 → 수정 플로우 성공</li>
 *   <li>INTG-SETTINGS-002: 직원 목록 조회 → 등록 → 수정 → 삭제 플로우 성공</li>
 *   <li>INTG-SETTINGS-003: 매장 정보 부분 수정 테스트</li>
 *   <li>INTG-SETTINGS-004: 직원 정보 부분 수정 테스트</li>
 *   <li>INTG-SETTINGS-005: 직원 삭제 시 관련 데이터 처리 확인 (Shift, AvailabilitySubmission)</li>
 *   <li>INTG-SETTINGS-006: 유효성 검증 실패 테스트 (fieldErrors 포함 검증)</li>
 * </ul>
 * 
 * <h3>요구사항 추적 (Traceability):</h3>
 * <ul>
 *   <li>{@code INTG-BE-Phase4-v1.1.0} - 설정 페이지 연동 (백엔드 작업 계획)</li>
 *   <li>{@code REQ-FUNC-002} - 매장 정보 수집</li>
 *   <li>{@code REQ-FUNC-003} - 직원 등록</li>
 * </ul>
 * 
 * @see <a href="../../../../../../SCON-Docs/BE-FE_Integration/INTG-BE-Phase4-settings.md">INTG-BE-Phase4-settings.md</a>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("설정 페이지 플로우 통합 테스트")
class SettingsIntegrationTest {

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

    @Autowired
    private ShiftRepository shiftRepository;

    @Test
    @DisplayName("INTG-SETTINGS-001: 매장 정보 조회 → 수정 플로우 성공")
    void settings_storeGetAndUpdateFlow_success() throws Exception {
        // Step 1: 회원가입 및 매장 생성
        SignupRequestDto signupRequest = SignupRequestDto.builder()
                .email("settings@example.com")
                .password("Password123!")
                .name("설정테스트사장")
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
        StoreRequestDto createRequest = StoreRequestDto.builder()
                .name("설정테스트매장")
                .businessType("베이커리카페")
                .address("서울시 강남구")
                .openTime(LocalTime.of(9, 0))
                .closeTime(LocalTime.of(18, 0))
                .storeHoliday(DayOfWeek.SATURDAY)
                .build();

        MvcResult storeResult = mockMvc.perform(post("/api/v1/stores")
                        .cookie(accessTokenCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        Long storeId = objectMapper.readTree(storeResult.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        // Step 2: 매장 정보 조회
        mockMvc.perform(get("/api/v1/stores/{id}", storeId)
                        .cookie(accessTokenCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("매장 조회 성공"))
                .andExpect(jsonPath("$.data.id").value(storeId.intValue()))
                .andExpect(jsonPath("$.data.name").value("설정테스트매장"))
                .andExpect(jsonPath("$.data.businessType").value("베이커리카페"));

        // Step 3: 매장 정보 수정
        StoreRequestDto updateRequest = StoreRequestDto.builder()
                .name("수정된매장명")
                .businessType("카페")
                .address("서울시 강남구 테헤란로 123")
                .openTime(LocalTime.of(8, 0))
                .closeTime(LocalTime.of(20, 0))
                .storeHoliday(DayOfWeek.SUNDAY)
                .build();

        mockMvc.perform(put("/api/v1/stores/{id}", storeId)
                        .cookie(accessTokenCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("매장 정보가 수정되었습니다"))
                .andExpect(jsonPath("$.data.name").value("수정된매장명"))
                .andExpect(jsonPath("$.data.businessType").value("카페"))
                .andExpect(jsonPath("$.data.address").value("서울시 강남구 테헤란로 123"))
                .andExpect(jsonPath("$.data.storeHoliday").value("SUNDAY"));

        // Step 4: 수정된 매장 정보 조회 확인
        mockMvc.perform(get("/api/v1/stores/{id}", storeId)
                        .cookie(accessTokenCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("수정된매장명"))
                .andExpect(jsonPath("$.data.businessType").value("카페"));
    }

    @Test
    @DisplayName("INTG-SETTINGS-002: 직원 목록 조회 → 등록 → 수정 → 삭제 플로우 성공")
    void settings_employeeFullFlow_success() throws Exception {
        // Step 1: 회원가입 및 매장 생성
        SignupRequestDto signupRequest = SignupRequestDto.builder()
                .email("employeeflow@example.com")
                .password("Password123!")
                .name("직원플로우테스트")
                .phone("010-3333-4444")
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
                .name("직원플로우매장")
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

        // Step 2: 직원 목록 조회 (빈 배열)
        mockMvc.perform(get("/api/v1/stores/{storeId}/employees", storeId)
                        .cookie(accessTokenCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));

        // Step 3: 직원 등록
        EmployeeRequestDto createRequest = EmployeeRequestDto.builder()
                .name("김직원")
                .phone("010-5555-6666")
                .hourlyWage(new BigDecimal("9860"))
                .employmentType(EmploymentType.EMPLOYEE)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/stores/{storeId}/employees", storeId)
                        .cookie(accessTokenCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        Long employeeId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        // Step 4: 직원 목록 조회 (1명)
        mockMvc.perform(get("/api/v1/stores/{storeId}/employees", storeId)
                        .cookie(accessTokenCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("김직원"))
                .andExpect(jsonPath("$.data[0].phone").value("010-5555-6666"));

        // Step 5: 직원 수정
        EmployeeRequestDto updateRequest = EmployeeRequestDto.builder()
                .name("김수정")
                .phone("010-7777-8888")
                .hourlyWage(new BigDecimal("12000"))
                .employmentType(EmploymentType.MANAGER)
                .build();

        mockMvc.perform(put("/api/v1/employees/{id}", employeeId)
                        .cookie(accessTokenCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("김수정"))
                .andExpect(jsonPath("$.data.phone").value("010-7777-8888"))
                .andExpect(jsonPath("$.data.hourlyWage").value(12000))
                .andExpect(jsonPath("$.data.employmentType").value("MANAGER"));

        // Step 6: 직원 삭제
        mockMvc.perform(delete("/api/v1/employees/{id}", employeeId)
                        .cookie(accessTokenCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("직원이 삭제되었습니다"));

        // Step 7: 삭제 확인 (직원 목록 조회 - 빈 배열)
        mockMvc.perform(get("/api/v1/stores/{storeId}/employees", storeId)
                        .cookie(accessTokenCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));

        // Step 8: 삭제 확인 (직원 조회 - 404)
        mockMvc.perform(get("/api/v1/employees/{id}", employeeId)
                        .cookie(accessTokenCookie))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("INTG-SETTINGS-003: 매장 정보 부분 수정 테스트")
    void settings_storePartialUpdate_success() throws Exception {
        // Step 1: 회원가입 및 매장 생성
        SignupRequestDto signupRequest = SignupRequestDto.builder()
                .email("partialupdate@example.com")
                .password("Password123!")
                .name("부분수정테스트")
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

        // 매장 생성 (모든 필드 포함)
        StoreRequestDto createRequest = StoreRequestDto.builder()
                .name("부분수정매장")
                .businessType("베이커리")
                .address("서울시 강남구")
                .openTime(LocalTime.of(9, 0))
                .closeTime(LocalTime.of(18, 0))
                .storeHoliday(DayOfWeek.SATURDAY)
                .build();

        MvcResult storeResult = mockMvc.perform(post("/api/v1/stores")
                        .cookie(accessTokenCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        Long storeId = objectMapper.readTree(storeResult.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        // Step 2: 부분 수정 (이름과 주소만 수정, 나머지는 기존 값 유지)
        // name은 @NotBlank로 필수이므로 포함해야 함
        // Store.update() 메서드가 null 체크를 통해 부분 업데이트를 지원
        // businessType, openTime, closeTime은 null 체크가 있으므로 null로 전달 시 업데이트 안 됨
        // storeHoliday는 null 체크가 없으므로 null로 전달 시 null로 업데이트됨 (주의)
        StoreRequestDto partialUpdateRequest = StoreRequestDto.builder()
                .name("부분수정된매장명") // 이름 수정 (필수)
                .address("서울시 강남구 테헤란로 456") // 주소만 수정
                .businessType(null) // 업종은 수정하지 않음 (null 체크로 인해 업데이트 안 됨)
                .openTime(null) // 영업시간은 수정하지 않음 (null 체크로 인해 업데이트 안 됨)
                .closeTime(null)
                .storeHoliday(DayOfWeek.SATURDAY) // 휴무일은 기존 값 유지 (null로 전달하면 null로 업데이트되므로 기존 값 전달)
                .build();

        mockMvc.perform(put("/api/v1/stores/{id}", storeId)
                        .cookie(accessTokenCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(partialUpdateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("부분수정된매장명"))
                .andExpect(jsonPath("$.data.address").value("서울시 강남구 테헤란로 456"))
                .andExpect(jsonPath("$.data.businessType").value("베이커리")) // 변경되지 않음 (null 체크로 인해)
                .andExpect(jsonPath("$.data.openTime").value("09:00:00")) // 변경되지 않음 (null 체크로 인해)
                .andExpect(jsonPath("$.data.closeTime").value("18:00:00")) // 변경되지 않음 (null 체크로 인해)
                .andExpect(jsonPath("$.data.storeHoliday").value("SATURDAY")); // 기존 값 유지
    }

    @Test
    @DisplayName("INTG-SETTINGS-004: 직원 정보 부분 수정 테스트")
    void settings_employeePartialUpdate_success() throws Exception {
        // Step 1: 회원가입 및 매장 생성
        SignupRequestDto signupRequest = SignupRequestDto.builder()
                .email("emppartial@example.com")
                .password("Password123!")
                .name("직원부분수정")
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
                .name("직원부분수정매장")
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

        // 직원 등록 (모든 필드 포함)
        EmployeeRequestDto createRequest = EmployeeRequestDto.builder()
                .name("이직원")
                .phone("010-1111-2222")
                .hourlyWage(new BigDecimal("9860"))
                .employmentType(EmploymentType.EMPLOYEE)
                .shiftPreset(ShiftPreset.MORNING)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/stores/{storeId}/employees", storeId)
                        .cookie(accessTokenCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        Long employeeId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        // Step 2: 부분 수정 (이름과 시급만 수정)
        // Employee.update() 메서드: name, phone, hourlyWage, employmentType, shiftPreset은 null 체크가 있음
        // employmentType은 @NotNull이므로 필수 (부분 수정 시에도 기존 값 전달 필요)
        // personalHoliday는 null 체크가 없으므로 null로 전달 시 null로 업데이트됨
        EmployeeRequestDto partialUpdateRequest = EmployeeRequestDto.builder()
                .name("이수정") // 이름 수정 (필수)
                .phone(null) // 전화번호는 수정하지 않음 (null 체크로 인해 업데이트 안 됨)
                .hourlyWage(new BigDecimal("12000")) // 시급만 수정
                .employmentType(EmploymentType.EMPLOYEE) // 고용형태는 @NotNull이므로 기존 값 전달 (필수)
                .shiftPreset(null) // 근무 프리셋은 수정하지 않음 (null 체크로 인해 업데이트 안 됨)
                .personalHoliday(null) // 휴무일은 null로 업데이트 (null 체크 없음)
                .build();

        // Step 3: 부분 수정 요청 (phone이 null이면 기존 값이 복호화되어 응답에 포함됨)
        mockMvc.perform(put("/api/v1/employees/{id}", employeeId)
                        .cookie(accessTokenCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(partialUpdateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("이수정"))
                .andExpect(jsonPath("$.data.hourlyWage").value(12000))
                .andExpect(jsonPath("$.data.phone").value("010-1111-2222")) // 기존 값 유지 (복호화됨)
                .andExpect(jsonPath("$.data.employmentType").value("EMPLOYEE")) // 변경되지 않음 (null 체크)
                .andExpect(jsonPath("$.data.shiftPreset").value("MORNING")); // 변경되지 않음 (null 체크)
    }

    @Test
    @DisplayName("INTG-SETTINGS-005: 직원 삭제 시 관련 데이터 처리 확인 (Shift, AvailabilitySubmission)")
    void settings_employeeDeleteCascade_success() throws Exception {
        // Step 1: 회원가입 및 매장 생성
        SignupRequestDto signupRequest = SignupRequestDto.builder()
                .email("cascade@example.com")
                .password("Password123!")
                .name("캐스케이드테스트")
                .phone("010-5656-7878")
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
                .name("캐스케이드매장")
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

        // 직원 등록
        EmployeeRequestDto employeeRequest = EmployeeRequestDto.builder()
                .name("캐스케이드직원")
                .phone("010-9999-1111")
                .hourlyWage(new BigDecimal("9860"))
                .employmentType(EmploymentType.EMPLOYEE)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/stores/{storeId}/employees", storeId)
                        .cookie(accessTokenCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(employeeRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        Long employeeId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        // Step 2: 관련 데이터 생성 (Schedule 및 Shift)
        Store store = storeRepository.findById(storeId).orElseThrow();
        Employee employee = employeeRepository.findById(employeeId).orElseThrow();
        
        // Schedule 생성
        Schedule schedule = Schedule.builder()
                .weekStartDate(LocalDate.now())
                .status(ScheduleStatus.DRAFT)
                .store(store)
                .build();
        schedule = scheduleRepository.save(schedule);
        scheduleRepository.flush(); // 즉시 DB에 반영
        
        // Shift 생성 (직원이 삭제되면 함께 삭제되어야 함 - cascade)
        // JPA cascade 삭제가 동작하려면 Employee의 shifts 컬렉션에 Shift가 포함되어 있어야 함
        // 하지만 Shift를 직접 생성하고 저장하면 Employee의 shifts 컬렉션에 자동으로 추가되지 않음
        // 따라서 양방향 관계를 명시적으로 설정해야 함 (리플렉션 사용 또는 직접 추가)
        Shift shift = Shift.builder()
                .workDate(LocalDate.now())
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(18, 0))
                .schedule(schedule)
                .employee(employee)
                .build();
        shift = shiftRepository.save(shift);
        
        // 양방향 관계 설정: Employee의 shifts 컬렉션에 추가
        // Employee 엔티티에 shifts 컬렉션이 private이므로 리플렉션 사용
        // 또는 Employee를 다시 조회하여 shifts 컬렉션을 로드하면 자동으로 포함됨
        employeeRepository.flush(); // 즉시 DB에 반영
        shiftRepository.flush(); // 즉시 DB에 반영
        
        // Employee를 다시 조회하여 shifts 컬렉션 로드 (LAZY 로딩)
        // findByEmployeeId 같은 쿼리로 Shift를 조회하면 자동으로 Employee의 컬렉션에 포함됨
        // 또는 Employee를 조회한 후 getShifts()를 호출하면 DB에서 조회됨
        employee = employeeRepository.findById(employeeId).orElseThrow();
        employee.getShifts().size(); // 컬렉션 초기화 (LAZY 로딩으로 Shift 조회)
        
        // Shift가 생성되었는지 확인
        Long shiftId = shift.getId();
        assertThat(shiftId).isNotNull();
        assertThat(shiftRepository.findById(shiftId)).isPresent();

        // Step 3: 직원 삭제
        mockMvc.perform(delete("/api/v1/employees/{id}", employeeId)
                        .cookie(accessTokenCookie))
                .andExpect(status().isOk());

        // Step 4: 관련 데이터 삭제 확인
        // EmployeeService.deleteEmployee()에서 employeeRepository.delete(employee)를 호출
        // Employee.getShifts().size()를 호출하여 컬렉션을 로드한 상태에서 삭제하면
        // JPA의 cascade = CascadeType.ALL, orphanRemoval = true로 인해 shifts도 함께 삭제됨
        
        // Employee 삭제 확인
        employeeRepository.flush(); // DB에 반영
        assertThat(employeeRepository.findById(employeeId)).isEmpty();
        
        // Cascade로 인해 Shift도 함께 삭제되었는지 확인
        // EmployeeService.deleteEmployee()에서 employee.getShifts().size()를 호출하여
        // 컬렉션을 로드한 상태에서 삭제하므로, cascade 삭제가 동작해야 함
        shiftRepository.flush(); // DB에 반영
        assertThat(shiftRepository.findById(shiftId)).isEmpty(); // Cascade로 인해 함께 삭제됨
    }

    @Test
    @DisplayName("INTG-SETTINGS-006: 유효성 검증 실패 테스트 (fieldErrors 포함 검증)")
    void settings_validationFailure_returnsFieldErrors() throws Exception {
        // Step 1: 회원가입 및 매장 생성
        SignupRequestDto signupRequest = SignupRequestDto.builder()
                .email("validation@example.com")
                .password("Password123!")
                .name("유효성검증테스트")
                .phone("010-8888-9999")
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
                .name("유효성검증매장")
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

        // Step 2: 매장 정보 수정 시 유효성 검증 실패 (name이 빈 문자열)
        StoreRequestDto invalidRequest = StoreRequestDto.builder()
                .name("") // @NotBlank 검증 실패
                .businessType("카페")
                .build();

        mockMvc.perform(put("/api/v1/stores/{id}", storeId)
                        .cookie(accessTokenCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.fieldErrors[0].field").exists())
                .andExpect(jsonPath("$.fieldErrors[0].message").exists());

        // Step 3: 직원 등록 시 유효성 검증 실패 (name이 null)
        EmployeeRequestDto invalidEmployeeRequest = EmployeeRequestDto.builder()
                .name(null) // @NotBlank 검증 실패 (DTO에서 확인 필요)
                .phone("010-1111-2222")
                .hourlyWage(new BigDecimal("9860"))
                .employmentType(EmploymentType.EMPLOYEE)
                .build();

        mockMvc.perform(post("/api/v1/stores/{storeId}/employees", storeId)
                        .cookie(accessTokenCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidEmployeeRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.fieldErrors[0].field").exists())
                .andExpect(jsonPath("$.fieldErrors[0].message").exists());
    }
}
