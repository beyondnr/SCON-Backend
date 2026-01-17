package vibe.scon.scon_backend.config.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.github.f4b6a3.ulid.UlidCreator;

import java.io.IOException;

/**
 * Request ID Tracking Filter
 * 
 * <p>프론트엔드로부터 전송된 X-Request-ID 헤더를 읽어 MDC에 저장하고,
 * 모든 로그에 Request ID를 포함시켜 전구간 로그 추적을 가능하게 합니다.</p>
 * 
 * <h3>동작 방식:</h3>
 * <ol>
 *   <li>요청 헤더에서 X-Request-ID 읽기 (없으면 서버에서 ULID 생성)</li>
 *   <li>MDC에 requestId 키로 저장</li>
 *   <li>응답 헤더에 X-Request-ID 포함</li>
 *   <li>요청 처리 완료 후 MDC 정리 (try-finally 사용)</li>
 * </ol>
 * 
 * <h3>필터 순서:</h3>
 * <p>이 필터는 가능한 한 필터 체인에서 먼저 실행되어야 합니다.
 * SecurityConfig에서 RateLimitingFilter와 JwtAuthenticationFilter 사이에 등록합니다.</p>
 * 
 * <h3>요구사항 추적 (Traceability):</h3>
 * <ul>
 *   <li>ULID 기반 Request ID 추적 시스템 구축</li>
 *   <li>REQ-NF-OPS: TraceId(MDC) 전파</li>
 * </ul>
 * 
 * @see <a href="../../SCON-Update-Plan/Logging/BE_ULID_Request_ID_Tracking_Implementation_Plan.md">BE_ULID_Request_ID_Tracking_Implementation_Plan</a>
 */
@Slf4j
@Component
public class RequestIdTrackingFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String MDC_REQUEST_ID_KEY = "requestId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // 요청 헤더에서 X-Request-ID 읽기 (없으면 서버에서 생성)
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.trim().isEmpty()) {
            // ULID는 시간 정보를 포함하므로 정렬에 유리함
            requestId = UlidCreator.getUlid().toString();
            log.debug("Request ID not found in header, generated new ULID: {}", requestId);
        }

        // MDC에 Request ID 저장
        MDC.put(MDC_REQUEST_ID_KEY, requestId);

        try {
            // 필터 체인 실행
            filterChain.doFilter(request, response);

            // 응답 헤더에 Request ID 포함 (선택사항)
            response.setHeader(REQUEST_ID_HEADER, requestId);

        } finally {
            // 요청 처리 완료 후 MDC 정리 (반드시 실행)
            // MDC는 스레드 로컬이므로, 정리하지 않으면 다른 요청의 로그에 이전 Request ID가 포함될 수 있음
            MDC.clear();
        }
    }
}
