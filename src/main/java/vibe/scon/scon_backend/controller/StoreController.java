package vibe.scon.scon_backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vibe.scon.scon_backend.dto.ApiResponse;
import vibe.scon.scon_backend.dto.async.AsyncTaskResponseDto;
import vibe.scon.scon_backend.dto.store.StoreRequestDto;
import vibe.scon.scon_backend.dto.store.StoreResponseDto;
import vibe.scon.scon_backend.entity.enums.TaskStatus;
import vibe.scon.scon_backend.service.AsyncStoreService;
import vibe.scon.scon_backend.service.AsyncTaskService;
import vibe.scon.scon_backend.service.StoreService;

import java.util.List;

/**
 * 매장 관리 API 컨트롤러.
 * 
 * <p>매장 CRUD API를 제공합니다.</p>
 * 
 * <h3>요구사항 추적 (Traceability):</h3>
 * <ul>
 *   <li>{@code REQ-FUNC-002} - 매장 정보 수집</li>
 *   <li>{@code AC-003} - 매장 정보 저장 및 조회</li>
 *   <li>{@code Issue-003 §9.2} - 매장 API 명세</li>
 * </ul>
 * 
 * <h3>API 엔드포인트:</h3>
 * <ul>
 *   <li>{@code POST /api/v1/stores} - 매장 생성</li>
 *   <li>{@code GET /api/v1/stores} - 내 매장 목록</li>
 *   <li>{@code GET /api/v1/stores/{id}} - 매장 상세</li>
 *   <li>{@code PUT /api/v1/stores/{id}} - 매장 수정</li>
 * </ul>
 * 
 * @see StoreService
 * @see <a href="tasks/github-issues/issue-003-REQ-FUNC-001-003.md">Issue-003 §9.2</a>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/stores")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;
    private final AsyncStoreService asyncStoreService;
    private final AsyncTaskService asyncTaskService;

    /**
     * 매장 생성 API.
     * 
     * <p>인증된 사장님의 새 매장을 생성합니다.</p>
     * 
     * <h4>TC-STORE-001 (매장 생성 API):</h4>
     * <ul>
     *   <li>Request: 매장명, 업종, 주소, 영업시간</li>
     *   <li>Response: 생성된 매장 정보</li>
     *   <li>HTTP 201 Created</li>
     * </ul>
     * 
     * @param authentication 인증 정보 (ownerId)
     * @param request 매장 생성 요청 DTO
     * @return 생성된 매장 응답 (201 Created)
     */
    @PostMapping
    public ResponseEntity<ApiResponse<StoreResponseDto>> createStore(
            Authentication authentication,
            @Valid @RequestBody StoreRequestDto request) {
        
        Long ownerId = (Long) authentication.getPrincipal();
        log.info("Create store request. ownerId: {}, storeName: {}", ownerId, request.getName());
        
        StoreResponseDto response = storeService.createStore(ownerId, request);
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created("매장이 생성되었습니다", response));
    }

    /**
     * 내 매장 목록 조회 API.
     * 
     * <p>인증된 사장님이 소유한 모든 매장 목록을 조회합니다.</p>
     * 
     * <h4>TC-STORE-005 (내 매장 목록 조회 API):</h4>
     * <ul>
     *   <li>Response: 본인 소유 매장 목록</li>
     *   <li>HTTP 200 OK</li>
     * </ul>
     * 
     * @param authentication 인증 정보 (ownerId)
     * @return 매장 목록 (200 OK)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<StoreResponseDto>>> getMyStores(
            Authentication authentication) {
        
        Long ownerId = (Long) authentication.getPrincipal();
        log.info("Get my stores request. ownerId: {}", ownerId);
        
        List<StoreResponseDto> response = storeService.getMyStores(ownerId);
        
        return ResponseEntity.ok(ApiResponse.success("매장 목록 조회 성공", response));
    }

    /**
     * 매장 상세 조회 API.
     * 
     * <p>본인 소유 매장의 상세 정보를 조회합니다.</p>
     * 
     * <h4>TC-STORE-002 (매장 조회 API):</h4>
     * <ul>
     *   <li>Response: 매장 상세 정보</li>
     *   <li>HTTP 200 OK</li>
     * </ul>
     * 
     * <h4>TC-STORE-006 (타 사용자 매장 접근 차단):</h4>
     * <ul>
     *   <li>타인 매장 접근 시 403 Forbidden</li>
     * </ul>
     * 
     * <h4>TC-STORE-007 (존재하지 않는 매장 조회):</h4>
     * <ul>
     *   <li>존재하지 않는 매장 조회 시 404 Not Found</li>
     * </ul>
     * 
     * @param authentication 인증 정보 (ownerId)
     * @param id 매장 ID
     * @return 매장 상세 정보 (200 OK)
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StoreResponseDto>> getStore(
            Authentication authentication,
            @PathVariable Long id) {
        
        Long ownerId = (Long) authentication.getPrincipal();
        log.info("Get store request. storeId: {}, ownerId: {}", id, ownerId);
        
        StoreResponseDto response = storeService.getStore(ownerId, id);
        
        return ResponseEntity.ok(ApiResponse.success("매장 조회 성공", response));
    }

    /**
     * 매장 정보 수정 API.
     * 
     * <p>본인 소유 매장의 정보를 수정합니다.</p>
     * 
     * <h4>TC-STORE-004 (매장 수정 API):</h4>
     * <ul>
     *   <li>Request: 수정할 매장 정보</li>
     *   <li>Response: 수정된 매장 정보</li>
     *   <li>HTTP 200 OK</li>
     * </ul>
     * 
     * @param authentication 인증 정보 (ownerId)
     * @param id 매장 ID
     * @param request 매장 수정 요청 DTO
     * @return 수정된 매장 정보 (200 OK)
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<StoreResponseDto>> updateStore(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody StoreRequestDto request) {
        
        Long ownerId = (Long) authentication.getPrincipal();
        log.info("Update store request. storeId: {}, ownerId: {}", id, ownerId);
        
        StoreResponseDto response = storeService.updateStore(ownerId, id, request);
        
        return ResponseEntity.ok(ApiResponse.success("매장 정보가 수정되었습니다", response));
    }
    
    /**
     * 매장 생성 (비동기).
     * 
     * <p>매장 생성 요청을 비동기로 처리합니다. 즉시 202 Accepted 응답을 반환하고,
     * 실제 작업은 백그라운드에서 진행됩니다.</p>
     * 
     * <h3>응답 플로우:</h3>
     * <ol>
     *   <li>요청 즉시: 202 Accepted + taskId 반환</li>
     *   <li>클라이언트: GET /api/v1/tasks/{taskId}로 상태 확인 (폴링)</li>
     *   <li>완료 후: GET /api/v1/tasks/{taskId}/result로 결과 조회</li>
     * </ol>
     * 
     * <h3>요구사항 추적:</h3>
     * <ul>
     *   <li>{@code Async Processing Plan Phase 3}: 매장 생성 비동기화</li>
     * </ul>
     * 
     * @param authentication 인증 정보 (ownerId)
     * @param request 매장 생성 요청 DTO
     * @return 작업 접수 응답 (202 Accepted)
     */
    @PostMapping("/async")
    public ResponseEntity<ApiResponse<AsyncTaskResponseDto>> createStoreAsync(
            Authentication authentication,
            @Valid @RequestBody StoreRequestDto request) {
        
        Long ownerId = (Long) authentication.getPrincipal();
        log.info("Create store async request. ownerId: {}, storeName: {}", ownerId, request.getName());
        
        // 작업 생성
        String taskId = asyncTaskService.createTask("STORE_CREATE", ownerId, request);
        
        // 비동기 작업 시작
        asyncStoreService.createStoreAsync(taskId, ownerId, request);
        
        // 즉시 응답 (202 Accepted)
        AsyncTaskResponseDto response = AsyncTaskResponseDto.builder()
                .taskId(taskId)
                .status(TaskStatus.IN_PROGRESS)
                .taskType("STORE_CREATE")
                .progress(0)
                .build();
        
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success("매장 생성 요청이 접수되었습니다", response));
    }
}
