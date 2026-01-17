package vibe.scon.scon_backend.config.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import vibe.scon.scon_backend.util.ApiLogger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Request/Response Logging Filter
 * 
 * 모든 HTTP 요청과 응답을 로깅합니다.
 * 
 * <h3>주의사항:</h3>
 * <ul>
 *   <li>요청/응답 본문을 읽기 위해 ContentCachingRequestWrapper/ResponseWrapper 사용</li>
 *   <li>필터 순서: SecurityConfig에서 명시적으로 등록 (JwtAuthenticationFilter 이후)</li>
 *   <li>성능 영향 최소화를 위해 개발 환경에서만 기본 활성화</li>
 *   <li>요청 본문 크기 제한: 1MB (설정 가능)</li>
 * </ul>
 * 
 * <h3>필터 등록:</h3>
 * <p>이 필터는 SecurityConfig에서 명시적으로 등록되어야 합니다.
 * SecurityConfig에서 addFilterAfter로 등록합니다.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class RequestResponseLoggingFilter extends OncePerRequestFilter {
    
    // 요청 본문 최대 크기 (1MB) - 큰 요청은 로깅하지 않음
    private static final int MAX_REQUEST_BODY_SIZE = 1024 * 1024;

    private final ApiLogger apiLogger;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        
        long startTime = System.currentTimeMillis();

        // 요청 본문 크기 확인 (너무 큰 요청은 Wrapper 생성하지 않음)
        int contentLength = request.getContentLength();
        if (contentLength > MAX_REQUEST_BODY_SIZE) {
            log.debug("Request body too large ({} bytes), skipping logging", contentLength);
            filterChain.doFilter(request, response);
            return;
        }

        // 요청 본문을 읽기 위해 Wrapper 사용
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        try {
            // 필터 체인 실행
            filterChain.doFilter(requestWrapper, responseWrapper);

            // 응답 시간 계산
            long duration = System.currentTimeMillis() - startTime;

            // 요청 본문 읽기
            byte[] requestBodyBytes = requestWrapper.getContentAsByteArray();
            String requestBody = null;
            if (requestBodyBytes.length > 0 && requestBodyBytes.length <= MAX_REQUEST_BODY_SIZE) {
                requestBody = new String(requestBodyBytes, StandardCharsets.UTF_8);
            }

            // 응답 본문 읽기
            byte[] responseBodyBytes = responseWrapper.getContentAsByteArray();
            String responseBody = null;
            if (responseBodyBytes.length > 0) {
                responseBody = new String(responseBodyBytes, StandardCharsets.UTF_8);
            }

            // 로깅
            apiLogger.logBackendRequest(requestWrapper, requestBody);
            apiLogger.logBackendResponse(requestWrapper, responseWrapper, responseBody, duration);

            // 응답 본문 복사 (Wrapper로 읽었으므로 다시 복사 필요)
            responseWrapper.copyBodyToResponse();

        } catch (Exception e) {
            log.error("Error in RequestResponseLoggingFilter", e);
            throw e;
        }
    }
}
