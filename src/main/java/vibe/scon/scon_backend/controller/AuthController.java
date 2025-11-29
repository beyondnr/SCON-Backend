package vibe.scon.scon_backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vibe.scon.scon_backend.dto.ApiResponse;
import vibe.scon.scon_backend.dto.auth.LoginRequestDto;
import vibe.scon.scon_backend.dto.auth.RefreshTokenRequestDto;
import vibe.scon.scon_backend.dto.auth.SignupRequestDto;
import vibe.scon.scon_backend.dto.auth.TokenResponseDto;
import vibe.scon.scon_backend.service.AuthService;

/**
 * 인증 API 컨트롤러.
 * 
 * <p>회원가입, 로그인, 토큰 갱신 API를 제공합니다.</p>
 * 
 * <h3>요구사항 추적 (Traceability):</h3>
 * <ul>
 *   <li>{@code REQ-FUNC-001} - 3단계 온보딩 마법사 (회원가입, 로그인)</li>
 *   <li>{@code Issue-003 §9.1} - 인증 API 명세</li>
 * </ul>
 * 
 * <h3>API 엔드포인트:</h3>
 * <ul>
 *   <li>{@code POST /api/v1/auth/signup} - 회원가입</li>
 *   <li>{@code POST /api/v1/auth/login} - 로그인</li>
 *   <li>{@code POST /api/v1/auth/refresh} - 토큰 갱신</li>
 * </ul>
 * 
 * @see AuthService
 * @see <a href="tasks/github-issues/issue-003-REQ-FUNC-001-003.md">Issue-003 §9.1</a>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 회원가입 API.
     * 
     * <p>신규 사장님 계정을 생성하고 JWT 토큰을 발급합니다.</p>
     * 
     * <h4>TC-AUTH-001 (회원가입 성공):</h4>
     * <ul>
     *   <li>Request: 이메일, 비밀번호, 이름, 전화번호</li>
     *   <li>Response: ownerId, accessToken, refreshToken</li>
     *   <li>HTTP 201 Created</li>
     * </ul>
     * 
     * <h4>TC-AUTH-003 (중복 이메일 가입 실패):</h4>
     * <ul>
     *   <li>이미 존재하는 이메일로 가입 시도</li>
     *   <li>HTTP 400 Bad Request</li>
     * </ul>
     * 
     * @param request 회원가입 요청 DTO
     * @return 토큰 응답 (201 Created)
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<TokenResponseDto>> signup(
            @Valid @RequestBody SignupRequestDto request) {
        
        log.info("Signup request received for email: {}", request.getEmail());
        
        TokenResponseDto response = authService.signup(request);
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("회원가입이 완료되었습니다", response));
    }

    /**
     * 로그인 API.
     * 
     * <p>이메일과 비밀번호를 검증하고 JWT 토큰을 발급합니다.</p>
     * 
     * <h4>TC-AUTH-004 (로그인 성공 + JWT 발급):</h4>
     * <ul>
     *   <li>Request: 이메일, 비밀번호</li>
     *   <li>Response: ownerId, accessToken (30분), refreshToken (7일)</li>
     *   <li>HTTP 200 OK</li>
     * </ul>
     * 
     * <h4>TC-AUTH-005 (잘못된 비밀번호 로그인 실패):</h4>
     * <ul>
     *   <li>잘못된 비밀번호로 로그인 시도</li>
     *   <li>HTTP 400 Bad Request</li>
     * </ul>
     * 
     * @param request 로그인 요청 DTO
     * @return 토큰 응답 (200 OK)
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponseDto>> login(
            @Valid @RequestBody LoginRequestDto request) {
        
        log.info("Login request received for email: {}", request.getEmail());
        
        TokenResponseDto response = authService.login(request);
        
        return ResponseEntity.ok(ApiResponse.success("로그인 성공", response));
    }

    /**
     * 토큰 갱신 API.
     * 
     * <p>유효한 Refresh Token으로 새 Access Token을 발급합니다.</p>
     * 
     * <h4>TC-AUTH-006 (토큰 갱신 API 성공):</h4>
     * <ul>
     *   <li>Request: refreshToken</li>
     *   <li>Response: 새로운 accessToken, refreshToken</li>
     *   <li>HTTP 200 OK</li>
     * </ul>
     * 
     * <h4>TC-AUTH-009 (만료된 Refresh Token 처리):</h4>
     * <ul>
     *   <li>만료된 Refresh Token으로 갱신 시도</li>
     *   <li>HTTP 400 Bad Request</li>
     * </ul>
     * 
     * @param request 토큰 갱신 요청 DTO
     * @return 토큰 응답 (200 OK)
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponseDto>> refreshToken(
            @Valid @RequestBody RefreshTokenRequestDto request) {
        
        log.info("Token refresh request received");
        
        TokenResponseDto response = authService.refreshToken(request);
        
        return ResponseEntity.ok(ApiResponse.success("토큰 갱신 성공", response));
    }
}
