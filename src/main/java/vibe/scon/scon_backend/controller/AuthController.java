package vibe.scon.scon_backend.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vibe.scon.scon_backend.dto.ApiResponse;
import vibe.scon.scon_backend.dto.auth.LoginRequestDto;
import vibe.scon.scon_backend.dto.auth.RefreshTokenRequestDto;
import vibe.scon.scon_backend.dto.auth.SignupRequestDto;
import vibe.scon.scon_backend.dto.auth.TokenResponseDto;
import vibe.scon.scon_backend.exception.BadRequestException;
import vibe.scon.scon_backend.service.AuthService;

/**
 * 인증 API 컨트롤러.
 * 
 * <p>회원가입, 로그인, 토큰 갱신 API를 제공합니다.
 * HttpOnly Cookie 방식으로 토큰을 전송하여 XSS 공격으로부터 보호합니다.</p>
 * 
 * <h3>요구사항 추적 (Traceability):</h3>
 * <ul>
 *   <li>{@code REQ-FUNC-001} - 3단계 온보딩 마법사 (회원가입, 로그인)</li>
 *   <li>{@code Issue-003 §9.1} - 인증 API 명세</li>
 *   <li>{@code POC-BE-SYNC-001} - HttpOnly Cookie 인증 및 API 경로 통일</li>
 * </ul>
 * 
 * <h3>API 엔드포인트:</h3>
 * <ul>
 *   <li>{@code POST /api/v1/auth/signup} - 회원가입</li>
 *   <li>{@code POST /api/v1/auth/login} - 로그인</li>
 *   <li>{@code POST /api/v1/auth/refresh} - 토큰 갱신</li>
 *   <li>{@code POST /api/v1/auth/logout} - 로그아웃</li>
 * </ul>
 * 
 * @see AuthService
 * @see <a href="tasks/github-issues/issue-003-REQ-FUNC-001-003.md">Issue-003 §9.1</a>
 * @see <a href="../../SCON-Update-Plan/POC-BE-FUNC-003-logout.md">POC-BE-FUNC-003</a>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final Environment environment;

    /**
     * 회원가입 API.
     * 
     * <p>신규 사장님 계정을 생성하고 JWT 토큰을 HttpOnly Cookie로 발급합니다.</p>
     * 
     * <h4>TC-AUTH-001 (회원가입 성공):</h4>
     * <ul>
     *   <li>Request: 이메일, 비밀번호, 이름, 전화번호</li>
     *   <li>Response: ownerId, email (토큰은 HttpOnly Cookie로 설정)</li>
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
     * @param response HTTP 응답 (Cookie 설정용)
     * @return 토큰 응답 (201 Created, 토큰은 Cookie로 전송)
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<TokenResponseDto>> signup(
            @Valid @RequestBody SignupRequestDto request,
            HttpServletResponse response) {
        
        log.info("Signup request received for email: {}", request.getEmail());
        
        TokenResponseDto tokenResponse = authService.signup(request);
        
        // HttpOnly Cookie로 토큰 설정
        setCookie(response, "accessToken", tokenResponse.getAccessToken(), 1800); // 30분
        setCookie(response, "refreshToken", tokenResponse.getRefreshToken(), 604800); // 7일
        
        // 응답 본문에서는 토큰 제거 (보안 강화)
        // ownerId와 email만 반환
        TokenResponseDto responseDto = TokenResponseDto.builder()
                .ownerId(tokenResponse.getOwnerId())
                .email(tokenResponse.getEmail())
                .build();
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("회원가입이 완료되었습니다", responseDto));
    }

    /**
     * 로그인 API.
     * 
     * <p>이메일과 비밀번호를 검증하고 JWT 토큰을 HttpOnly Cookie로 발급합니다.</p>
     * 
     * <h4>TC-AUTH-004 (로그인 성공 + JWT 발급):</h4>
     * <ul>
     *   <li>Request: 이메일, 비밀번호</li>
     *   <li>Response: ownerId, email (토큰은 HttpOnly Cookie로 설정)</li>
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
     * @param response HTTP 응답 (Cookie 설정용)
     * @return 토큰 응답 (200 OK, 토큰은 Cookie로 전송)
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponseDto>> login(
            @Valid @RequestBody LoginRequestDto request,
            HttpServletResponse response) {
        
        log.info("Login request received for email: {}", request.getEmail());
        
        TokenResponseDto tokenResponse = authService.login(request);
        
        // HttpOnly Cookie로 토큰 설정
        setCookie(response, "accessToken", tokenResponse.getAccessToken(), 1800); // 30분
        setCookie(response, "refreshToken", tokenResponse.getRefreshToken(), 604800); // 7일
        
        // 응답 본문에서는 토큰 제거 (보안 강화)
        // ownerId와 email만 반환
        TokenResponseDto responseDto = TokenResponseDto.builder()
                .ownerId(tokenResponse.getOwnerId())
                .email(tokenResponse.getEmail())
                .build();
        
        return ResponseEntity.ok(ApiResponse.success("로그인 성공", responseDto));
    }

    /**
     * 토큰 갱신 API.
     * 
     * <p>Cookie에서 Refresh Token을 읽어 새 Access Token과 Refresh Token을 HttpOnly Cookie로 발급합니다.</p>
     * 
     * <h4>TC-AUTH-006 (토큰 갱신 API 성공):</h4>
     * <ul>
     *   <li>Request: Cookie에서 refreshToken 자동 전송</li>
     *   <li>Response: ownerId, email (새 토큰은 HttpOnly Cookie로 설정)</li>
     *   <li>HTTP 200 OK</li>
     * </ul>
     * 
     * <h4>TC-AUTH-009 (만료된 Refresh Token 처리):</h4>
     * <ul>
     *   <li>만료된 Refresh Token으로 갱신 시도</li>
     *   <li>HTTP 400 Bad Request</li>
     * </ul>
     * 
     * @param request HTTP 요청 (Cookie에서 refreshToken 추출용)
     * @param response HTTP 응답 (Cookie 설정용)
     * @return 토큰 응답 (200 OK, 새 토큰은 Cookie로 전송)
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponseDto>> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response) {
        
        log.info("Token refresh request received");
        
        // Cookie에서 refreshToken 추출
        String refreshToken = extractRefreshTokenFromCookie(request);
        
        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new BadRequestException("Refresh Token이 필요합니다");
        }
        
        // AuthService 호출 (기존 로직 유지)
        TokenResponseDto tokenResponse = authService.refreshToken(
                RefreshTokenRequestDto.builder()
                        .refreshToken(refreshToken)
                        .build()
        );
        
        // 새 토큰을 Cookie로 설정
        setCookie(response, "accessToken", tokenResponse.getAccessToken(), 1800); // 30분
        setCookie(response, "refreshToken", tokenResponse.getRefreshToken(), 604800); // 7일
        
        // 응답 본문에서는 토큰 제거
        TokenResponseDto responseDto = TokenResponseDto.builder()
                .ownerId(tokenResponse.getOwnerId())
                .email(tokenResponse.getEmail())
                .build();
        
        return ResponseEntity.ok(ApiResponse.success("토큰 갱신 성공", responseDto));
    }

    /**
     * 로그아웃 API.
     * 
     * <p>Cookie에서 Refresh Token을 읽어 무효화하고, 모든 인증 Cookie를 삭제합니다.</p>
     * 
     * <h4>TC-AUTH-010 (로그아웃 성공):</h4>
     * <ul>
     *   <li>Request: Cookie에서 refreshToken 자동 전송</li>
     *   <li>Response: 200 OK</li>
     *   <li>Refresh Token이 DB에서 삭제됨</li>
     *   <li>모든 인증 Cookie 삭제됨</li>
     * </ul>
     * 
     * <h4>TC-AUTH-011 (로그아웃 후 토큰 갱신 실패):</h4>
     * <ul>
     *   <li>로그아웃 후 같은 Refresh Token으로 갱신 시도</li>
     *   <li>HTTP 400 Bad Request</li>
     * </ul>
     * 
     * <h4>TC-AUTH-012 (유효하지 않은 토큰으로 로그아웃 실패):</h4>
     * <ul>
     *   <li>유효하지 않은 Refresh Token으로 로그아웃 시도</li>
     *   <li>HTTP 400 Bad Request</li>
     * </ul>
     * 
     * @param request HTTP 요청 (Cookie에서 refreshToken 추출용)
     * @param response HTTP 응답 (Cookie 삭제용)
     * @return 성공 응답 (200 OK)
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response) {
        
        log.info("Logout request received");
        
        // Cookie에서 refreshToken 추출
        String refreshToken = extractRefreshTokenFromCookie(request);
        
        // refreshToken이 있으면 DB에서 삭제
        if (refreshToken != null && !refreshToken.isEmpty()) {
            authService.logout(
                    RefreshTokenRequestDto.builder()
                            .refreshToken(refreshToken)
                            .build()
            );
        }
        
        // Cookie 삭제 (MaxAge를 0으로 설정)
        deleteCookie(response, "accessToken");
        deleteCookie(response, "refreshToken");
        
        return ResponseEntity.ok(ApiResponse.success("로그아웃이 완료되었습니다", null));
    }
    
    /**
     * HttpOnly Cookie 설정 유틸리티 메서드.
     * 
     * <p>환경별 Secure 플래그를 자동으로 설정합니다.
     * Spring의 ResponseCookie를 사용하여 SameSite 속성을 올바르게 설정합니다.</p>
     * 
     * <h4>INTG-BE-Phase2-v1.1.0 (2026-01-03):</h4>
     * <ul>
     *   <li>Cookie SameSite 설정 개선: ResponseCookie 사용으로 변경</li>
     *   <li>기존 setAttribute 방식은 Java Servlet API에서 SameSite 속성이 제대로 설정되지 않을 수 있음</li>
     * </ul>
     * 
     * @param response HTTP 응답
     * @param name Cookie 이름
     * @param value Cookie 값
     * @param maxAge Cookie 만료 시간 (초 단위)
     */
    private void setCookie(HttpServletResponse response, String name, String value, int maxAge) {
        // 프로파일별 Secure 설정
        String activeProfile = environment.getProperty("spring.profiles.active", "dev");
        boolean isProduction = "prod".equals(activeProfile) || "production".equals(activeProfile);
        
        // Spring의 ResponseCookie를 사용하여 SameSite 속성을 올바르게 설정
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(isProduction)
                .path("/")
                .maxAge(maxAge)
                .sameSite("Strict")
                .build();
        
        // Set-Cookie 헤더로 추가 (ResponseCookie의 toString()이 올바른 형식으로 변환)
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
    
    /**
     * Cookie 삭제 유틸리티 메서드.
     * 
     * <p>Cookie를 삭제하기 위해 MaxAge를 0으로 설정하고 SameSite 속성을 포함합니다.</p>
     * 
     * <h4>INTG-BE-Phase2-v1.1.0 (2026-01-03):</h4>
     * <ul>
     *   <li>ResponseCookie 사용으로 변경하여 일관성 유지</li>
     * </ul>
     * 
     * @param response HTTP 응답
     * @param name 삭제할 Cookie 이름
     */
    private void deleteCookie(HttpServletResponse response, String name) {
        // 프로파일별 Secure 설정
        String activeProfile = environment.getProperty("spring.profiles.active", "dev");
        boolean isProduction = "prod".equals(activeProfile) || "production".equals(activeProfile);
        
        // ResponseCookie를 사용하여 삭제 (MaxAge=0)
        ResponseCookie cookie = ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(isProduction)
                .path("/")
                .maxAge(0)
                .sameSite("Strict")
                .build();
        
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
    
    /**
     * Cookie에서 refreshToken 추출.
     * 
     * @param request HTTP 요청
     * @return 추출된 refreshToken, 없으면 null
     */
    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("refreshToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
