package vibe.scon.scon_backend.config.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import vibe.scon.scon_backend.dto.ErrorResponse;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate Limiting 필터.
 * 
 * <p>무차별 대입 공격(Brute Force Attack)을 방어하기 위한 Rate Limiting을 구현합니다.
 * IP 기반으로 요청 횟수를 제한합니다.</p>
 * 
 * <h3>Rate Limit 설정:</h3>
 * <ul>
 *   <li>최대 요청 수: 5회/분 (설정 가능)</li>
 *   <li>적용 대상: /api/v1/auth/login, /api/v1/auth/signup</li>
 * </ul>
 * 
 * <h3>요구사항 추적 (Traceability):</h3>
 * <ul>
 *   <li>{@code POC-BE-SEC-002} - 백엔드 보안 강화 (Rate Limiting)</li>
 * </ul>
 * 
 * <h3>주의사항:</h3>
 * <ul>
 *   <li>현재는 메모리 기반 구현 (개발/테스트용)</li>
 *   <li>프로덕션 환경에서는 Redis 기반 Rate Limiting 사용 권장</li>
 * </ul>
 * 
 * @see <a href="../../SCON-Update-Plan/POC-BE-SEC-002.md">POC-BE-SEC-002</a>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private final Environment environment;
    private final ObjectMapper objectMapper;

    // IP별 요청 횟수 추적 (메모리 기반, 프로덕션에서는 Redis 사용 권장)
    private final Map<String, RateLimitInfo> rateLimitMap = new ConcurrentHashMap<>();
    
    // Rate Limit 설정
    private static final int MAX_REQUESTS = 5; // 최대 요청 수
    private static final long TIME_WINDOW_MS = 60000; // 1분 (밀리초)
    
    // Rate Limiting 적용 대상 경로
    private static final List<String> RATE_LIMIT_PATHS = Arrays.asList(
            "/api/v1/auth/login",
            "/api/v1/auth/signup"
    );
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        
        // 테스트 환경에서는 Rate Limiting 비활성화
        String[] activeProfiles = environment.getActiveProfiles();
        boolean isTestProfile = java.util.Arrays.asList(activeProfiles).contains("test");
        if (isTestProfile) {
            filterChain.doFilter(request, response);
            return;
        }
        
        String path = request.getRequestURI();
        
        // Rate Limiting 적용 대상 경로인지 확인
        boolean shouldRateLimit = RATE_LIMIT_PATHS.stream()
                .anyMatch(path::startsWith);
        
        if (shouldRateLimit) {
            String clientIp = getClientIp(request);
            RateLimitInfo info = rateLimitMap.computeIfAbsent(
                    clientIp,
                    k -> new RateLimitInfo()
            );
            
            long now = System.currentTimeMillis();
            
            // 시간 윈도우 초과 시 리셋
            if (now - info.getFirstRequestTime() > TIME_WINDOW_MS) {
                info.reset();
            }
            
            // 요청 횟수 증가
            info.increment();
            
            // Rate Limit 초과 시
            if (info.getCount() > MAX_REQUESTS) {
                log.warn("Rate limit exceeded for IP: {}, path: {}", clientIp, path);
                
                // 표준 ErrorResponse 형식으로 응답 (INTG-BE-Phase2-v1.1.0)
                ErrorResponse errorResponse = ErrorResponse.of(
                        429,
                        "TOO_MANY_REQUESTS",
                        "너무 많은 요청입니다. 잠시 후 다시 시도해주세요.",
                        path
                );
                
                response.setStatus(429); // HttpStatus.TOO_MANY_REQUESTS (429)
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding("UTF-8");
                
                try {
                    String jsonResponse = objectMapper.writeValueAsString(errorResponse);
                    response.getWriter().write(jsonResponse);
                } catch (Exception e) {
                    log.error("Failed to serialize ErrorResponse to JSON", e);
                    // 폴백: 간단한 JSON 응답
                    response.getWriter().write(
                            "{\"status\":429,\"error\":\"TOO_MANY_REQUESTS\"," +
                            "\"message\":\"너무 많은 요청입니다. 잠시 후 다시 시도해주세요.\"," +
                            "\"path\":\"" + path + "\",\"timestamp\":\"" +
                            java.time.LocalDateTime.now() + "\"}"
                    );
                }
                return;
            }
        }
        
        filterChain.doFilter(request, response);
    }
    
    /**
     * 클라이언트 IP 주소 추출.
     * 
     * <p>프록시나 로드밸런서를 통한 요청도 고려하여 X-Forwarded-For 헤더를 확인합니다.</p>
     * 
     * @param request HTTP 요청
     * @return 클라이언트 IP 주소
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For는 여러 IP가 쉼표로 구분될 수 있음 (첫 번째가 실제 클라이언트 IP)
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp.trim();
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * Rate Limit 정보를 저장하는 내부 클래스.
     */
    @Data
    private static class RateLimitInfo {
        private int count = 0;
        private long firstRequestTime = System.currentTimeMillis();
        
        /**
         * 요청 횟수 증가.
         */
        public void increment() {
            count++;
        }
        
        /**
         * Rate Limit 정보 리셋.
         */
        public void reset() {
            count = 1;
            firstRequestTime = System.currentTimeMillis();
        }
    }
}

