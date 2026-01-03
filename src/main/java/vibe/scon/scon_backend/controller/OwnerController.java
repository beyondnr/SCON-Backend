package vibe.scon.scon_backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vibe.scon.scon_backend.dto.ApiResponse;
import vibe.scon.scon_backend.dto.owner.OwnerProfileResponse;
import vibe.scon.scon_backend.dto.owner.UpdateOwnerProfileRequest;
import vibe.scon.scon_backend.service.OwnerService;

/**
 * 사용자(Owner) 관리 API 컨트롤러.
 * 
 * <p>마이페이지에서 사용자 프로필 조회 및 수정 API를 제공합니다.</p>
 * 
 * <h3>요구사항 추적 (Traceability):</h3>
 * <ul>
 *   <li>{@code POC-BE-FUNC-002} - 마이페이지 사용자 프로필 조회/수정 API</li>
 * </ul>
 * 
 * <h3>API 엔드포인트:</h3>
 * <ul>
 *   <li>{@code GET /api/v1/owners/me} - 프로필 조회</li>
 *   <li>{@code PATCH /api/v1/owners/me} - 프로필 수정</li>
 * </ul>
 * 
 * @see OwnerService
 * @see <a href="../../SCON-Update-Plan/POC-BE-FUNC-002-mypage.md">POC-BE-FUNC-002</a>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/owners")
@RequiredArgsConstructor
public class OwnerController {

    private final OwnerService ownerService;

    /**
     * 현재 로그인한 사용자 프로필 조회 API.
     * 
     * <p>JWT 토큰에서 추출한 ownerId로 사용자 정보를 조회합니다.</p>
     * 
     * <h4>TC-OWNER-001 (프로필 조회 성공):</h4>
     * <ul>
     *   <li>Request: 인증 토큰 (Header)</li>
     *   <li>Response: ownerId, email, name, phone, createdAt, updatedAt</li>
     *   <li>HTTP 200 OK</li>
     * </ul>
     * 
     * <h4>TC-OWNER-003 (인증 실패):</h4>
     * <ul>
     *   <li>토큰 없이 호출 시 HTTP 401 Unauthorized</li>
     * </ul>
     * 
     * <h4>TC-OWNER-004 (사용자 없음):</h4>
     * <ul>
     *   <li>존재하지 않는 사용자 조회 시 HTTP 404 Not Found</li>
     * </ul>
     * 
     * @param authentication 인증 정보 (ownerId)
     * @return 사용자 프로필 응답 (200 OK)
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<OwnerProfileResponse>> getCurrentOwnerProfile(
            Authentication authentication
    ) {
        Long ownerId = (Long) authentication.getPrincipal();
        log.info("Get owner profile request. ownerId: {}", ownerId);

        OwnerProfileResponse response = ownerService.getCurrentOwnerProfile(ownerId);
        return ResponseEntity.ok(ApiResponse.success("사용자 정보 조회 성공", response));
    }

    /**
     * 현재 로그인한 사용자 프로필 수정 API.
     * 
     * <p>사용자의 이름과 전화번호를 수정합니다. 이메일은 변경할 수 없습니다.</p>
     * 
     * <h4>TC-OWNER-002 (프로필 수정 성공):</h4>
     * <ul>
     *   <li>Request: name, phone (선택)</li>
     *   <li>Response: 수정된 사용자 프로필 정보</li>
     *   <li>HTTP 200 OK</li>
     * </ul>
     * 
     * <h4>TC-OWNER-005 (name만 수정):</h4>
     * <ul>
     *   <li>Request Body: {"name": "새로운 이름"}</li>
     *   <li>name만 업데이트되고 phone은 변경되지 않음</li>
     * </ul>
     * 
     * <h4>TC-OWNER-006 (phone만 수정):</h4>
     * <ul>
     *   <li>Request Body: {"phone": "010-9876-5432"}</li>
     *   <li>phone만 업데이트되고 name은 변경되지 않음</li>
     * </ul>
     * 
     * <h4>TC-OWNER-007 (name을 빈 문자열로 설정 시도):</h4>
     * <ul>
     *   <li>Request Body: {"name": ""}</li>
     *   <li>HTTP 400 Bad Request</li>
     *   <li>에러 메시지: "이름은 빈 문자열일 수 없습니다"</li>
     * </ul>
     * 
     * <h4>TC-OWNER-008 (phone을 null로 설정):</h4>
     * <ul>
     *   <li>Request Body: {"phone": ""}</li>
     *   <li>phone이 null로 설정됨</li>
     * </ul>
     * 
     * <h4>TC-OWNER-009 (유효성 검증 실패):</h4>
     * <ul>
     *   <li>name이 100자 초과 시 HTTP 400 Bad Request</li>
     *   <li>phone이 20자 초과 시 HTTP 400 Bad Request</li>
     * </ul>
     * 
     * @param authentication 인증 정보 (ownerId)
     * @param request 프로필 수정 요청 DTO
     * @return 수정된 사용자 프로필 응답 (200 OK)
     */
    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<OwnerProfileResponse>> updateCurrentOwnerProfile(
            @Valid @RequestBody UpdateOwnerProfileRequest request,
            Authentication authentication
    ) {
        Long ownerId = (Long) authentication.getPrincipal();
        log.info("Update owner profile request. ownerId: {}", ownerId);

        OwnerProfileResponse response = ownerService.updateOwnerProfile(ownerId, request);
        return ResponseEntity.ok(ApiResponse.success("사용자 정보가 수정되었습니다", response));
    }
}

