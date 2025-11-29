package vibe.scon.scon_backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vibe.scon.scon_backend.dto.ApiResponse;
import vibe.scon.scon_backend.dto.onboarding.OnboardingCompleteRequestDto;
import vibe.scon.scon_backend.dto.onboarding.OnboardingCompleteResponseDto;
import vibe.scon.scon_backend.service.OnboardingService;

/**
 * 온보딩 API 컨트롤러.
 * 
 * <p>온보딩 완료 API를 제공합니다.</p>
 * 
 * <h3>요구사항 추적 (Traceability):</h3>
 * <ul>
 *   <li>{@code REQ-FUNC-001} - 3단계 온보딩 마법사</li>
 *   <li>{@code AC-005} - 온보딩 완료 및 첫 스케줄 생성</li>
 *   <li>{@code Issue-003 §9.4} - 온보딩 API 명세</li>
 * </ul>
 * 
 * <h3>API 엔드포인트:</h3>
 * <ul>
 *   <li>{@code POST /api/v1/onboarding/complete} - 온보딩 완료</li>
 * </ul>
 * 
 * @see OnboardingService
 * @see <a href="tasks/github-issues/issue-003-REQ-FUNC-001-003.md">Issue-003 §9.4</a>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;

    /**
     * 온보딩 완료 API.
     * 
     * <p>온보딩을 완료하고 첫 스케줄(Draft)을 자동 생성합니다.</p>
     * 
     * <h4>TC-ONBOARD-001 (온보딩 플로우 전체):</h4>
     * <ul>
     *   <li>회원가입 → 매장 설정 → 온보딩 완료</li>
     * </ul>
     * 
     * <h4>TC-ONBOARD-002 (Draft 스케줄 자동 생성):</h4>
     * <ul>
     *   <li>현재 주차의 Draft 스케줄 생성</li>
     *   <li>대시보드에서 조회 가능</li>
     * </ul>
     * 
     * @param authentication 인증 정보 (ownerId)
     * @param request 온보딩 완료 요청 DTO
     * @return 온보딩 완료 응답 (200 OK)
     */
    @PostMapping("/complete")
    public ResponseEntity<ApiResponse<OnboardingCompleteResponseDto>> completeOnboarding(
            Authentication authentication,
            @Valid @RequestBody OnboardingCompleteRequestDto request) {
        
        Long ownerId = (Long) authentication.getPrincipal();
        log.info("Onboarding complete request. ownerId: {}, storeId: {}", ownerId, request.getStoreId());
        
        OnboardingCompleteResponseDto response = onboardingService.completeOnboarding(ownerId, request);
        
        return ResponseEntity.ok(ApiResponse.success("온보딩이 완료되었습니다", response));
    }
}
