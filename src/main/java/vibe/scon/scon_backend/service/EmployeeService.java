package vibe.scon.scon_backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vibe.scon.scon_backend.dto.employee.EmployeeRequestDto;
import vibe.scon.scon_backend.dto.employee.EmployeeResponseDto;
import vibe.scon.scon_backend.entity.Employee;
import vibe.scon.scon_backend.entity.Store;
import vibe.scon.scon_backend.exception.ForbiddenException;
import vibe.scon.scon_backend.exception.ResourceNotFoundException;
import vibe.scon.scon_backend.repository.AvailabilitySubmissionRepository;
import vibe.scon.scon_backend.repository.EmployeeRepository;
import vibe.scon.scon_backend.repository.ShiftRepository;
import vibe.scon.scon_backend.repository.StoreRepository;
import vibe.scon.scon_backend.util.EncryptionUtil;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 직원 관리 서비스.
 * 
 * <p>직원 CRUD 및 PII 암호화/복호화 비즈니스 로직을 처리합니다.</p>
 * 
 * <h3>요구사항 추적 (Traceability):</h3>
 * <ul>
 *   <li>{@code REQ-FUNC-003} - 직원 등록</li>
 *   <li>{@code REQ-NF-007} - 저장 데이터 암호화 (AES-256)</li>
 *   <li>{@code AC-004} - 직원 등록 및 PII 암호화</li>
 *   <li>{@code TC-EMP-002} - PII 암호화 저장 검증</li>
 *   <li>{@code TC-EMP-003} - PII 복호화 응답 검증</li>
 * </ul>
 * 
 * @see EncryptionUtil
 * @see <a href="tasks/github-issues/issue-003-REQ-FUNC-001-003.md">Issue-003 §9.3</a>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final StoreRepository storeRepository;
    private final ShiftRepository shiftRepository;
    private final AvailabilitySubmissionRepository availabilitySubmissionRepository;
    private final EncryptionUtil encryptionUtil;

    /**
     * 직원 등록.
     * 
     * <p>새 직원을 등록합니다. phone 필드는 AES-256으로 암호화하여 저장합니다.</p>
     * 
     * <h4>AC-004, TC-EMP-001, TC-EMP-002 검증:</h4>
     * <ul>
     *   <li>고유한 Employee ID 생성</li>
     *   <li>Store와 FK로 연결</li>
     *   <li>phone 필드 AES-256-GCM 암호화 저장</li>
     * </ul>
     * 
     * @param ownerId 인증된 Owner ID
     * @param storeId 매장 ID
     * @param request 직원 등록 요청 DTO
     * @return 등록된 직원 응답 DTO (phone 복호화)
     */
    @Transactional
    public EmployeeResponseDto createEmployee(Long ownerId, Long storeId, EmployeeRequestDto request) {
        log.info("Creating employee. ownerId: {}, storeId: {}, employeeName: {}", 
                ownerId, storeId, request.getName());

        // 매장 조회 및 소유권 확인
        Store store = getStoreAndValidateOwnership(storeId, ownerId);

        // phone 암호화 (REQ-NF-007)
        String encryptedPhone = null;
        if (request.getPhone() != null && !request.getPhone().isEmpty()) {
            encryptedPhone = encryptionUtil.encrypt(request.getPhone());
            log.debug("Phone encrypted for employee: {}", request.getName());
        }

        // Employee 엔티티 생성
        Employee employee = Employee.builder()
                .name(request.getName())
                .phone(encryptedPhone)  // 암호화된 phone 저장
                .email(request.getEmail())
                .hourlyWage(request.getHourlyWage())
                .employmentType(request.getEmploymentType())
                .shiftPreset(request.getShiftPreset())
                .customShiftStartTime(request.getCustomShiftStartTime())
                .customShiftEndTime(request.getCustomShiftEndTime())
                .personalHoliday(request.getPersonalHoliday())
                .store(store)
                .build();

        Employee savedEmployee = employeeRepository.save(employee);
        log.info("Employee created. employeeId: {}, storeId: {}", savedEmployee.getId(), storeId);

        // Mock 메일 발송 로직
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            log.info("[Mock] 개인정보 수집 출처 안내 메일 발송. To: {}", request.getEmail());
        }

        // 응답 시 복호화된 phone 반환 (TC-EMP-003)
        return EmployeeResponseDto.from(savedEmployee, request.getPhone());
    }

    /**
     * 직원 상세 조회.
     * 
     * <p>직원 ID로 상세 정보를 조회합니다. phone은 복호화하여 반환합니다.</p>
     * 
     * <h4>TC-EMP-003, TC-EMP-007, TC-EMP-008 검증:</h4>
     * <ul>
     *   <li>PII 복호화 응답</li>
     *   <li>타 사용자 직원 접근 차단</li>
     *   <li>존재하지 않는 직원 조회 시 404</li>
     * </ul>
     * 
     * @param ownerId 인증된 Owner ID
     * @param employeeId 직원 ID
     * @return 직원 응답 DTO (phone 복호화)
     */
    public EmployeeResponseDto getEmployee(Long ownerId, Long employeeId) {
        log.debug("Getting employee. employeeId: {}, ownerId: {}", employeeId, ownerId);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("직원을 찾을 수 없습니다: " + employeeId));

        // 소유권 확인 (TC-EMP-007)
        validateEmployeeOwnership(employee, ownerId);

        // phone 복호화 (TC-EMP-003)
        String decryptedPhone = decryptPhone(employee.getPhone());

        return EmployeeResponseDto.from(employee, decryptedPhone);
    }

    /**
     * 매장별 직원 목록 조회.
     * 
     * <p>특정 매장에 소속된 모든 직원 목록을 조회합니다.</p>
     * 
     * <h4>TC-EMP-006 검증:</h4>
     * <ul>
     *   <li>매장 소속 직원 목록 반환</li>
     * </ul>
     * 
     * @param ownerId 인증된 Owner ID
     * @param storeId 매장 ID
     * @return 직원 목록 (phone 복호화)
     */
    public List<EmployeeResponseDto> getEmployeesByStore(Long ownerId, Long storeId) {
        log.debug("Getting employees for store. storeId: {}, ownerId: {}", storeId, ownerId);

        // 매장 소유권 확인 (1차 검증)
        getStoreAndValidateOwnership(storeId, ownerId);

        // ownerId를 포함한 쿼리로 변경 (2차 검증 - 방어적 프로그래밍)
        // POC-BE-SEC-001: 데이터 격리 및 접근 제어 개선
        List<Employee> employees = employeeRepository.findByStoreIdAndOwnerId(storeId, ownerId);

        return employees.stream()
                .map(employee -> EmployeeResponseDto.from(employee, decryptPhone(employee.getPhone())))
                .collect(Collectors.toList());
    }

    /**
     * 직원 정보 수정.
     * 
     * <p>직원 정보를 수정합니다. phone 변경 시 암호화하여 저장합니다.</p>
     * 
     * <h4>TC-EMP-004 검증:</h4>
     * <ul>
     *   <li>직원 정보 수정 성공</li>
     * </ul>
     * 
     * @param ownerId 인증된 Owner ID
     * @param employeeId 직원 ID
     * @param request 직원 수정 요청 DTO
     * @return 수정된 직원 응답 DTO
     */
    @Transactional
    public EmployeeResponseDto updateEmployee(Long ownerId, Long employeeId, EmployeeRequestDto request) {
        log.info("Updating employee. employeeId: {}, ownerId: {}", employeeId, ownerId);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("직원을 찾을 수 없습니다: " + employeeId));

        // 소유권 확인
        validateEmployeeOwnership(employee, ownerId);

        // phone 암호화 (변경된 경우)
        String encryptedPhone = null;
        String responsePhone = request.getPhone();
        if (request.getPhone() != null && !request.getPhone().isEmpty()) {
            encryptedPhone = encryptionUtil.encrypt(request.getPhone());
        } else {
            // phone이 null이면 기존 값을 복호화하여 응답에 포함 (부분 수정 지원)
            // Employee.update()에서 null 체크로 인해 기존 값이 유지되지만, 응답에서는 복호화된 값 전달
            responsePhone = decryptPhone(employee.getPhone());
        }

        // 직원 정보 업데이트
        employee.update(
                request.getName(),
                encryptedPhone,
                request.getEmail(),
                request.getHourlyWage(),
                request.getEmploymentType(),
                request.getShiftPreset(),
                request.getCustomShiftStartTime(),
                request.getCustomShiftEndTime(),
                request.getPersonalHoliday()
        );

        log.info("Employee updated. employeeId: {}", employeeId);

        return EmployeeResponseDto.from(employee, responsePhone);
    }

    /**
     * 직원 삭제 (Soft Delete).
     * 
     * <p>직원을 삭제합니다. 실제로는 DB에서 삭제됩니다.</p>
     * 
     * <h4>TC-EMP-005 검증:</h4>
     * <ul>
     *   <li>직원 삭제 성공</li>
     * </ul>
     * 
     * @param ownerId 인증된 Owner ID
     * @param employeeId 직원 ID
     */
    @Transactional
    public void deleteEmployee(Long ownerId, Long employeeId) {
        log.info("Deleting employee. employeeId: {}, ownerId: {}", employeeId, ownerId);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("직원을 찾을 수 없습니다: " + employeeId));

        // 소유권 확인
        validateEmployeeOwnership(employee, ownerId);

        // 관련 데이터 삭제 (cascade로 자동 삭제되지만, 명시적으로 삭제하여 확실하게 처리)
        // Employee 엔티티에 cascade = CascadeType.ALL, orphanRemoval = true 설정되어 있지만,
        // Shift를 직접 생성하고 저장하면 Employee의 shifts 컬렉션에 자동으로 추가되지 않을 수 있음
        // 따라서 명시적으로 관련 데이터를 삭제하여 데이터 정합성을 보장
        // INTG-BE-Phase4-v1.1.0: 직원 삭제 시 관련 데이터 처리 확인
        
        // Shift 삭제 (외래키 제약조건을 고려하여 명시적으로 삭제)
        shiftRepository.findByEmployeeId(employeeId).forEach(shiftRepository::delete);
        
        // AvailabilitySubmission 삭제
        availabilitySubmissionRepository.findByEmployeeId(employeeId)
                .forEach(availabilitySubmissionRepository::delete);
        
        employeeRepository.delete(employee);
        log.info("Employee deleted. employeeId: {}", employeeId);
    }

    /**
     * 매장 조회 및 소유권 검증.
     * 
     * @param storeId 매장 ID
     * @param ownerId Owner ID
     * @return Store 엔티티
     */
    private Store getStoreAndValidateOwnership(Long storeId, Long ownerId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("매장을 찾을 수 없습니다: " + storeId));

        if (!store.getOwner().getId().equals(ownerId)) {
            log.warn("Unauthorized store access. storeId: {}, requestedBy: {}", storeId, ownerId);
            throw new ForbiddenException("해당 매장에 대한 접근 권한이 없습니다");
        }

        return store;
    }

    /**
     * 직원 소유권 검증.
     * 
     * @param employee 검증할 직원
     * @param ownerId Owner ID
     */
    private void validateEmployeeOwnership(Employee employee, Long ownerId) {
        if (!employee.getStore().getOwner().getId().equals(ownerId)) {
            log.warn("Unauthorized employee access. employeeId: {}, requestedBy: {}", 
                    employee.getId(), ownerId);
            throw new ForbiddenException("해당 직원에 대한 접근 권한이 없습니다");
        }
    }

    /**
     * phone 복호화.
     * 
     * <p>암호화된 phone을 복호화합니다. null이거나 빈 문자열인 경우 그대로 반환합니다.</p>
     * 
     * @param encryptedPhone 암호화된 phone
     * @return 복호화된 phone
     */
    private String decryptPhone(String encryptedPhone) {
        if (encryptedPhone == null || encryptedPhone.isEmpty()) {
            return encryptedPhone;
        }
        
        try {
            return encryptionUtil.decrypt(encryptedPhone);
        } catch (Exception e) {
            log.error("Failed to decrypt phone: {}", e.getMessage());
            return null;  // 복호화 실패 시 null 반환
        }
    }
}
