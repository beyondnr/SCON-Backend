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
import vibe.scon.scon_backend.dto.employee.EmployeeRequestDto;
import vibe.scon.scon_backend.dto.store.StoreRequestDto;
import vibe.scon.scon_backend.entity.enums.EmploymentType;
import vibe.scon.scon_backend.entity.enums.ShiftPreset;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * EmployeeController 통합 테스트.
 * 
 * <h3>테스트 케이스 추적:</h3>
 * <ul>
 *   <li>TC-EMP-001: 직원 등록 API</li>
 *   <li>TC-EMP-002: PII 암호화 저장 검증 (REQ-NF-007)</li>
 *   <li>TC-EMP-003: PII 복호화 응답 검증</li>
 *   <li>TC-EMP-004: 직원 수정 API</li>
 *   <li>TC-EMP-005: 직원 삭제 (Soft Delete)</li>
 *   <li>TC-EMP-006: 직원 목록 조회 API</li>
 *   <li>TC-EMP-007: 타 사용자 직원 접근 차단 (403)</li>
 *   <li>TC-EMP-008: 존재하지 않는 직원 조회 (404)</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("EmployeeController 통합 테스트")
class EmployeeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String accessToken;
    private Long storeId;

    @BeforeEach
    void setUp() throws Exception {
        // 회원가입
        SignupRequestDto signupRequest = SignupRequestDto.builder()
                .email("emptest@example.com")
                .password("Password123!")
                .name("직원테스트")
                .phone("010-3333-4444")
                .build();

        MvcResult authResult = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        // Cookie에서 accessToken 추출
        jakarta.servlet.http.Cookie[] cookies = authResult.getResponse().getCookies();
        if (cookies != null) {
            for (jakarta.servlet.http.Cookie cookie : cookies) {
                if ("accessToken".equals(cookie.getName())) {
                    accessToken = cookie.getValue();
                    break;
                }
            }
        }
        
        // Cookie에서 토큰을 찾지 못한 경우 null로 설정 (하위 호환성을 위해 Bearer Token도 지원)
        if (accessToken == null) {
            // 응답 본문에서 토큰 추출 시도 (하위 호환성)
            try {
                accessToken = objectMapper.readTree(authResult.getResponse().getContentAsString())
                        .get("data").get("accessToken").asText();
            } catch (Exception e) {
                // 토큰이 응답 본문에 없는 경우 (Cookie 방식)
                accessToken = null;
            }
        }

        // 매장 생성 - 201 Created 반환
        StoreRequestDto storeRequest = StoreRequestDto.builder()
                .name("직원테스트매장")
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
    @DisplayName("TC-EMP-001: 직원 등록 API 성공")
    void createEmployee_success() throws Exception {
        // Given
        EmployeeRequestDto employeeRequest = EmployeeRequestDto.builder()
                .name("김알바")
                .phone("010-5555-6666")
                .hourlyWage(new BigDecimal("9860"))
                .employmentType(EmploymentType.EMPLOYEE)
                .shiftPreset(ShiftPreset.MORNING) // 새 필드 테스트
                .personalHoliday(DayOfWeek.TUESDAY) // 개인 휴무일 추가
                .build();

        // When & Then - 직원 생성은 201 Created 반환
        mockMvc.perform(post("/api/v1/stores/{storeId}/employees", storeId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(employeeRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.name").value("김알바"))
                .andExpect(jsonPath("$.data.employmentType").value("EMPLOYEE"))
                .andExpect(jsonPath("$.data.shiftPreset").value("MORNING"))
                .andExpect(jsonPath("$.data.personalHoliday").value("TUESDAY")); // 개인 휴무일 검증
    }

    @Test
    @DisplayName("TC-EMP-003: PII 복호화 응답 검증 - phone 필드가 복호화되어 응답")
    void createAndGetEmployee_phoneDecrypted() throws Exception {
        // Given - 직원 생성
        String originalPhone = "010-7777-8888";
        EmployeeRequestDto employeeRequest = EmployeeRequestDto.builder()
                .name("이알바")
                .phone(originalPhone)
                .hourlyWage(new BigDecimal("10000"))
                .employmentType(EmploymentType.MANAGER)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/stores/{storeId}/employees", storeId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(employeeRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        Long employeeId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        // When & Then - 조회 시 phone이 복호화되어 반환되는지 확인
        mockMvc.perform(get("/api/v1/employees/{id}", employeeId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phone").value(originalPhone));
    }

    @Test
    @DisplayName("TC-EMP-004: 직원 수정 API 성공 - 커스텀 시프트 반영")
    void updateEmployee_success() throws Exception {
        // Given - 직원 생성
        EmployeeRequestDto createRequest = EmployeeRequestDto.builder()
                .name("원래이름")
                .phone("010-1111-1111")
                .hourlyWage(new BigDecimal("9860"))
                .employmentType(EmploymentType.EMPLOYEE)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/stores/{storeId}/employees", storeId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        Long employeeId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        // When & Then - 수정 (커스텀 시프트 추가)
        EmployeeRequestDto updateRequest = EmployeeRequestDto.builder()
                .name("변경된이름")
                .phone("010-2222-2222")
                .hourlyWage(new BigDecimal("12000"))
                .employmentType(EmploymentType.MANAGER)
                .shiftPreset(ShiftPreset.CUSTOM)
                .customShiftStartTime(LocalTime.of(9, 0))
                .customShiftEndTime(LocalTime.of(18, 0))
                .personalHoliday(DayOfWeek.WEDNESDAY) // 휴무일 수정
                .build();

        mockMvc.perform(put("/api/v1/employees/{id}", employeeId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("변경된이름"))
                .andExpect(jsonPath("$.data.phone").value("010-2222-2222"))
                .andExpect(jsonPath("$.data.employmentType").value("MANAGER"))
                .andExpect(jsonPath("$.data.shiftPreset").value("CUSTOM"))
                .andExpect(jsonPath("$.data.customShiftStartTime").value("09:00:00"))
                .andExpect(jsonPath("$.data.customShiftEndTime").value("18:00:00"))
                .andExpect(jsonPath("$.data.personalHoliday").value("WEDNESDAY")); // 수정된 휴무일 검증
    }

    @Test
    @DisplayName("TC-EMP-005: 직원 삭제 API 성공")
    void deleteEmployee_success() throws Exception {
        // Given - 직원 생성
        EmployeeRequestDto employeeRequest = EmployeeRequestDto.builder()
                .name("삭제대상")
                .phone("010-9999-9999")
                .hourlyWage(new BigDecimal("9860"))
                .employmentType(EmploymentType.EMPLOYEE)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/stores/{storeId}/employees", storeId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(employeeRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        Long employeeId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        // When - 삭제
        mockMvc.perform(delete("/api/v1/employees/{id}", employeeId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // Then - 조회 시 404
        mockMvc.perform(get("/api/v1/employees/{id}", employeeId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("TC-EMP-006: 직원 목록 조회 API 성공")
    void getEmployees_success() throws Exception {
        // Given - 직원 2명 생성
        EmployeeRequestDto emp1 = EmployeeRequestDto.builder()
                .name("직원1").phone("010-1111-1111")
                .hourlyWage(new BigDecimal("9860")).employmentType(EmploymentType.EMPLOYEE)
                .build();
        EmployeeRequestDto emp2 = EmployeeRequestDto.builder()
                .name("직원2").phone("010-2222-2222")
                .hourlyWage(new BigDecimal("10000")).employmentType(EmploymentType.MANAGER)
                .build();

        mockMvc.perform(post("/api/v1/stores/{storeId}/employees", storeId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emp1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/stores/{storeId}/employees", storeId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emp2)))
                .andExpect(status().isCreated());

        // When & Then
        mockMvc.perform(get("/api/v1/stores/{storeId}/employees", storeId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    @DisplayName("TC-EMP-008: 존재하지 않는 직원 조회 시 404")
    void getEmployee_notFound_returns404() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/employees/{id}", 99999L)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("인증 없이 직원 등록 시 401")
    void createEmployee_withoutAuth_returns401() throws Exception {
        // Given
        EmployeeRequestDto employeeRequest = EmployeeRequestDto.builder()
                .name("무인증직원")
                .phone("010-0000-0000")
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/stores/{storeId}/employees", storeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(employeeRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("TC-EMP-007: 타 사용자 직원 접근 차단 - 다른 사용자의 storeId로 조회 시 403 반환")
    void getEmployeesByStore_otherUserStore_returns403() throws Exception {
        // Given - 다른 사용자 계정 생성
        SignupRequestDto otherUserSignup = SignupRequestDto.builder()
                .email("otheruser@example.com")
                .password("Password123!")
                .name("다른사용자")
                .phone("010-9999-9999")
                .build();

        MvcResult otherUserResult = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(otherUserSignup)))
                .andExpect(status().isCreated())
                .andReturn();

        // Cookie에서 accessToken 추출
        String otherUserToken = null;
        jakarta.servlet.http.Cookie[] otherUserCookies = otherUserResult.getResponse().getCookies();
        if (otherUserCookies != null) {
            for (jakarta.servlet.http.Cookie cookie : otherUserCookies) {
                if ("accessToken".equals(cookie.getName())) {
                    otherUserToken = cookie.getValue();
                    break;
                }
            }
        }
        
        // Cookie에서 토큰을 찾지 못한 경우 응답 본문에서 추출 시도 (하위 호환성)
        if (otherUserToken == null) {
            try {
                otherUserToken = objectMapper.readTree(otherUserResult.getResponse().getContentAsString())
                        .get("data").get("accessToken").asText();
            } catch (Exception e) {
                // 토큰이 응답 본문에 없는 경우 (Cookie 방식)
                otherUserToken = null;
            }
        }

        // 다른 사용자의 매장 생성
        StoreRequestDto otherStoreRequest = StoreRequestDto.builder()
                .name("다른사용자매장")
                .businessType("베이커리")
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
        mockMvc.perform(get("/api/v1/stores/{storeId}/employees", otherStoreId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden());
    }
}
