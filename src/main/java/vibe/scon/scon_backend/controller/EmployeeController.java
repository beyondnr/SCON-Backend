package vibe.scon.scon_backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vibe.scon.scon_backend.dto.ApiResponse;
import vibe.scon.scon_backend.dto.employee.EmployeeRequestDto;
import vibe.scon.scon_backend.dto.employee.EmployeeResponseDto;
import vibe.scon.scon_backend.service.EmployeeService;

import java.util.List;

/**
 * 직원 관리 API 컨트롤러.
 * 
 * <p>직원 CRUD API를 제공합니다.</p>
 * 
 * <h3>요구사항 추적 (Traceability):</h3>
 * <ul>
 *   <li>{@code REQ-FUNC-003} - 직원 등록</li>
 *   <li>{@code AC-004} - 직원 등록 및 PII 암호화</li>
 *   <li>{@code Issue-003 §9.3} - 직원 API 명세</li>
 * </ul>
 * 
 * <h3>API 엔드포인트:</h3>
 * <ul>
 *   <li>{@code POST /api/v1/stores/{storeId}/employees} - 직원 등록</li>
 *   <li>{@code GET /api/v1/stores/{storeId}/employees} - 직원 목록</li>
 *   <li>{@code GET /api/v1/employees/{id}} - 직원 상세</li>
 *   <li>{@code PUT /api/v1/employees/{id}} - 직원 수정</li>
 *   <li>{@code DELETE /api/v1/employees/{id}} - 직원 삭제</li>
 * </ul>
 * 
 * @see EmployeeService
 * @see <a href="tasks/github-issues/issue-003-REQ-FUNC-001-003.md">Issue-003 §9.3</a>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    /**
     * 직원 등록 API.
     * 
     * <p>특정 매장에 새 직원을 등록합니다. phone은 암호화하여 저장됩니다.</p>
     * 
     * <h4>TC-EMP-001 (직원 등록 API):</h4>
     * <ul>
     *   <li>Request: 이름, 연락처, 시급, 고용형태</li>
     *   <li>Response: 등록된 직원 정보</li>
     *   <li>HTTP 201 Created</li>
     * </ul>
     * 
     * <h4>TC-EMP-002 (PII 암호화 저장 검증):</h4>
     * <ul>
     *   <li>phone 필드 AES-256-GCM 암호화 저장</li>
     * </ul>
     * 
     * @param authentication 인증 정보 (ownerId)
     * @param storeId 매장 ID
     * @param request 직원 등록 요청 DTO
     * @return 등록된 직원 응답 (201 Created)
     */
    @PostMapping("/api/v1/stores/{storeId}/employees")
    public ResponseEntity<ApiResponse<EmployeeResponseDto>> createEmployee(
            Authentication authentication,
            @PathVariable Long storeId,
            @Valid @RequestBody EmployeeRequestDto request) {
        
        Long ownerId = (Long) authentication.getPrincipal();
        log.info("Create employee request. ownerId: {}, storeId: {}, employeeName: {}", 
                ownerId, storeId, request.getName());
        
        EmployeeResponseDto response = employeeService.createEmployee(ownerId, storeId, request);
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created("직원이 등록되었습니다", response));
    }

    /**
     * 매장별 직원 목록 조회 API.
     * 
     * <p>특정 매장에 소속된 모든 직원 목록을 조회합니다.</p>
     * 
     * <h4>TC-EMP-006 (직원 목록 조회 API):</h4>
     * <ul>
     *   <li>Response: 매장 소속 직원 목록</li>
     *   <li>HTTP 200 OK</li>
     * </ul>
     * 
     * @param authentication 인증 정보 (ownerId)
     * @param storeId 매장 ID
     * @return 직원 목록 (200 OK)
     */
    @GetMapping("/api/v1/stores/{storeId}/employees")
    public ResponseEntity<ApiResponse<List<EmployeeResponseDto>>> getEmployeesByStore(
            Authentication authentication,
            @PathVariable Long storeId) {
        
        Long ownerId = (Long) authentication.getPrincipal();
        log.info("Get employees request. ownerId: {}, storeId: {}", ownerId, storeId);
        
        List<EmployeeResponseDto> response = employeeService.getEmployeesByStore(ownerId, storeId);
        
        return ResponseEntity.ok(ApiResponse.success("직원 목록 조회 성공", response));
    }

    /**
     * 직원 상세 조회 API.
     * 
     * <p>직원 ID로 상세 정보를 조회합니다. phone은 복호화하여 반환합니다.</p>
     * 
     * <h4>TC-EMP-003 (PII 복호화 응답 검증):</h4>
     * <ul>
     *   <li>phone 필드 복호화하여 응답</li>
     *   <li>HTTP 200 OK</li>
     * </ul>
     * 
     * <h4>TC-EMP-007 (타 사용자 직원 접근 차단):</h4>
     * <ul>
     *   <li>타인 직원 접근 시 403 Forbidden</li>
     * </ul>
     * 
     * <h4>TC-EMP-008 (존재하지 않는 직원 조회):</h4>
     * <ul>
     *   <li>존재하지 않는 직원 조회 시 404 Not Found</li>
     * </ul>
     * 
     * @param authentication 인증 정보 (ownerId)
     * @param id 직원 ID
     * @return 직원 상세 정보 (200 OK)
     */
    @GetMapping("/api/v1/employees/{id}")
    public ResponseEntity<ApiResponse<EmployeeResponseDto>> getEmployee(
            Authentication authentication,
            @PathVariable Long id) {
        
        Long ownerId = (Long) authentication.getPrincipal();
        log.info("Get employee request. ownerId: {}, employeeId: {}", ownerId, id);
        
        EmployeeResponseDto response = employeeService.getEmployee(ownerId, id);
        
        return ResponseEntity.ok(ApiResponse.success("직원 조회 성공", response));
    }

    /**
     * 직원 정보 수정 API.
     * 
     * <p>직원 정보를 수정합니다. phone 변경 시 암호화하여 저장됩니다.</p>
     * 
     * <h4>TC-EMP-004 (직원 수정 API):</h4>
     * <ul>
     *   <li>Request: 수정할 직원 정보</li>
     *   <li>Response: 수정된 직원 정보</li>
     *   <li>HTTP 200 OK</li>
     * </ul>
     * 
     * @param authentication 인증 정보 (ownerId)
     * @param id 직원 ID
     * @param request 직원 수정 요청 DTO
     * @return 수정된 직원 정보 (200 OK)
     */
    @PutMapping("/api/v1/employees/{id}")
    public ResponseEntity<ApiResponse<EmployeeResponseDto>> updateEmployee(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody EmployeeRequestDto request) {
        
        Long ownerId = (Long) authentication.getPrincipal();
        log.info("Update employee request. ownerId: {}, employeeId: {}", ownerId, id);
        
        EmployeeResponseDto response = employeeService.updateEmployee(ownerId, id, request);
        
        return ResponseEntity.ok(ApiResponse.success("직원 정보가 수정되었습니다", response));
    }

    /**
     * 직원 삭제 API.
     * 
     * <h4>TC-EMP-005 (직원 삭제):</h4>
     * <ul>
     *   <li>직원 삭제 성공</li>
     *   <li>HTTP 200 OK</li>
     * </ul>
     * 
     * @param authentication 인증 정보 (ownerId)
     * @param id 직원 ID
     * @return 삭제 성공 메시지 (200 OK)
     */
    @DeleteMapping("/api/v1/employees/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteEmployee(
            Authentication authentication,
            @PathVariable Long id) {
        
        Long ownerId = (Long) authentication.getPrincipal();
        log.info("Delete employee request. ownerId: {}, employeeId: {}", ownerId, id);
        
        employeeService.deleteEmployee(ownerId, id);
        
        return ResponseEntity.ok(ApiResponse.success("직원이 삭제되었습니다"));
    }
}
