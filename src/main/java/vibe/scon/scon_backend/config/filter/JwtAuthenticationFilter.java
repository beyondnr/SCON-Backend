package vibe.scon.scon_backend.config.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import vibe.scon.scon_backend.util.JwtTokenProvider;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT 인증 필터.
 * 
 * <p>HTTP 요청의 Cookie 또는 Authorization 헤더에서 JWT 토큰을 추출하고 검증합니다.
 * 유효한 토큰인 경우 SecurityContext에 인증 정보를 설정합니다.</p>
 * 
 * <h3>요구사항 추적 (Traceability):</h3>
 * <ul>
 *   <li>{@code REQ-FUNC-001} - 3단계 온보딩 마법사 (JWT 인증)</li>
 *   <li>{@code TC-AUTH-007} - 만료된 Access Token 처리</li>
 *   <li>{@code TC-AUTH-008} - 유효하지 않은 토큰 처리</li>
 *   <li>{@code POC-BE-SYNC-001} - HttpOnly Cookie 인증 지원</li>
 * </ul>
 * 
 * <h3>처리 흐름:</h3>
 * <ol>
 *   <li>Cookie에서 accessToken 추출 시도 (우선)</li>
 *   <li>없으면 Authorization 헤더에서 Bearer 토큰 추출 (하위 호환성)</li>
 *   <li>토큰 유효성 검증 (서명, 만료)</li>
 *   <li>유효한 경우 SecurityContext에 인증 정보 설정</li>
 *   <li>다음 필터로 요청 전달</li>
 * </ol>
 * 
 * @see JwtTokenProvider
 * @see <a href="tasks/github-issues/issue-003-REQ-FUNC-001-003.md">Issue-003</a>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * JWT 토큰 검증 및 인증 처리.
     * 
     * @param request HTTP 요청
     * @param response HTTP 응답
     * @param filterChain 필터 체인
     * @throws ServletException 서블릿 예외
     * @throws IOException IO 예외
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        try {
            // 1. Authorization 헤더에서 토큰 추출
            String token = extractTokenFromRequest(request);

            // 2. 토큰이 존재하고 유효한 경우
            if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
                
                // 3. Access Token인지 확인 (Refresh Token으로 API 접근 방지)
                if (!jwtTokenProvider.isAccessToken(token)) {
                    log.warn("Attempted to use refresh token for API access");
                    filterChain.doFilter(request, response);
                    return;
                }

                // 4. 토큰에서 사용자 정보 추출
                Long ownerId = jwtTokenProvider.getOwnerIdFromToken(token);
                String email = jwtTokenProvider.getEmailFromToken(token);

                // 5. Authentication 객체 생성
                // Principal로 ownerId를 사용하고, email은 credentials에 저장
                UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(
                                ownerId,                    // principal (ownerId)
                                email,                      // credentials (email)
                                Collections.singletonList(  // authorities
                                        new SimpleGrantedAuthority("ROLE_OWNER")
                                )
                        );

                // 6. SecurityContext에 인증 정보 설정
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                log.debug("Set Authentication to SecurityContext for ownerId: {}, uri: {}", 
                        ownerId, request.getRequestURI());
            }

        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
            // 인증 실패 시 SecurityContext를 초기화하여 익명 사용자로 처리
            SecurityContextHolder.clearContext();
        }

        // 7. 다음 필터로 요청 전달
        filterChain.doFilter(request, response);
    }

    /**
     * HTTP 요청에서 JWT 토큰 추출.
     * 
     * <p>Cookie에서 accessToken을 먼저 확인하고, 없으면 Authorization 헤더에서 추출합니다.
     * 하위 호환성을 위해 Authorization 헤더도 계속 지원합니다.</p>
     * 
     * <h3>우선순위:</h3>
     * <ol>
     *   <li>Cookie에서 accessToken 추출 (우선)</li>
     *   <li>Authorization 헤더에서 Bearer 토큰 추출 (하위 호환성)</li>
     * </ol>
     * 
     * @param request HTTP 요청
     * @return 추출된 토큰 문자열, 없으면 null
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        // 1. Cookie에서 accessToken 추출 시도 (우선)
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("accessToken".equals(cookie.getName())) {
                    String token = cookie.getValue();
                    if (StringUtils.hasText(token)) {
                        log.debug("Token extracted from Cookie");
                        return token;
                    }
                }
            }
        }
        
        // 2. Authorization 헤더에서 토큰 추출 (하위 호환성)
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            log.debug("Token extracted from Authorization header");
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        
        return null;
    }

    /**
     * 특정 경로는 필터 적용 제외.
     * 
     * <p>공개 API 경로는 JWT 검증을 스킵합니다.</p>
     * 
     * @param request HTTP 요청
     * @return 필터 적용 제외 여부
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        
        // 공개 API 경로는 필터 스킵
        return path.startsWith("/api/v1/auth/") ||
               path.equals("/health") ||
               path.equals("/api/health") ||
               path.equals("/api/v1/health") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/v3/api-docs");
    }
}
