package vibe.scon.scon_backend.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API Request/Response Logging Utility
 * 
 * 2가지 로거 타입 제공:
 * - logBackendRequest: 백엔드 요청 로깅
 * - logBackendResponse: 백엔드 응답 로깅
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiLogger {

    private final ObjectMapper objectMapper;

    @Value("${app.logging.enable-request-logging:true}")
    private boolean enableRequestLogging;

    @Value("${app.logging.enable-response-logging:true}")
    private boolean enableResponseLogging;

    // 로깅 제외 경로 목록
    private static final List<String> EXCLUDED_PATHS = Arrays.asList(
            "/api/v1/health",
            "/api/v1/health/ping",
            "/actuator"
    );

    // 민감 정보 필드 목록 (대소문자 구분 없이 처리)
    private static final List<String> SENSITIVE_FIELDS = Arrays.asList(
            "password",
            "accesstoken",  // 소문자로 통일하여 비교
            "refreshtoken",
            "token",
            "authorization"
    );

    /**
     * 해당 경로가 로깅 제외 대상인지 확인
     */
    private boolean isExcludedPath(String path) {
        return EXCLUDED_PATHS.stream().anyMatch(path::startsWith);
    }

    /**
     * 민감 정보를 마스킹한 객체 반환
     */
    @SuppressWarnings("unchecked")
    private Object sanitizeData(Object data) {
        if (data == null) {
            return null;
        }

        if (data instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) data;
            Map<String, Object> sanitized = new HashMap<>(map);
            
            for (String key : SENSITIVE_FIELDS) {
                if (sanitized.containsKey(key)) {
                    sanitized.put(key, "[REDACTED]");
                }
            }
            
            return sanitized;
        }

        return data;
    }

    /**
     * 헤더에서 민감 정보 제거 (대소문자 무시)
     */
    private Map<String, String> sanitizeHeaders(Map<String, String> headers) {
        if (headers == null) {
            return Collections.emptyMap();
        }

        Map<String, String> sanitized = new HashMap<>(headers);
        // 헤더 키는 대소문자를 구분하지 않으므로 소문자로 변환하여 비교
        SENSITIVE_FIELDS.forEach(sensitiveKey -> {
            // 원본 키를 찾아서 마스킹
            headers.keySet().forEach(originalKey -> {
                if (originalKey.toLowerCase().equals(sensitiveKey)) {
                    sanitized.put(originalKey, "[REDACTED]");
                }
            });
        });

        return sanitized;
    }

    /**
     * 클라이언트 IP 주소 추출
     * 
     * <p>참고: RateLimitingFilter에도 동일한 메서드가 있으므로, 
     * 공통 유틸리티로 분리하는 것을 권장합니다 (HttpRequestUtil.java).</p>
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            // X-Forwarded-For는 여러 IP가 쉼표로 구분될 수 있음 (첫 번째가 실제 클라이언트 IP)
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp.trim();
        }
        
        return request.getRemoteAddr();
    }

    /**
     * 헤더를 Map으로 변환
     */
    private Map<String, String> getHeadersMap(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Collections.list(request.getHeaderNames())
                .forEach(name -> headers.put(name, request.getHeader(name)));
        return headers;
    }

    /**
     * 백엔드 요청 로깅
     */
    public void logBackendRequest(HttpServletRequest request, String requestBody) {
        if (!enableRequestLogging) {
            return;
        }

        String path = request.getRequestURI();
        if (isExcludedPath(path)) {
            return;
        }

        try {
            Map<String, Object> logData = new HashMap<>();
            logData.put("timestamp", java.time.LocalDateTime.now().toString());
            logData.put("type", "BACKEND_REQUEST");
            logData.put("method", request.getMethod());
            logData.put("uri", path);
            logData.put("queryString", request.getQueryString());
            logData.put("clientIp", getClientIp(request));
            logData.put("headers", sanitizeHeaders(getHeadersMap(request)));
            
            // Request ID 추가 (MDC에서 읽기)
            String requestId = MDC.get("requestId");
            if (requestId != null) {
                logData.put("requestId", requestId);
            }
            
            // 요청 본문 파싱 및 마스킹
            if (requestBody != null && !requestBody.isEmpty()) {
                try {
                    Object bodyObj = objectMapper.readValue(requestBody, Object.class);
                    logData.put("body", sanitizeData(bodyObj));
                } catch (Exception e) {
                    logData.put("body", "[Unable to parse JSON]");
                }
            }

            log.info("API Request: {}", objectMapper.writeValueAsString(logData));
        } catch (Exception e) {
            log.warn("Failed to log request: {}", e.getMessage());
        }
    }

    /**
     * 백엔드 응답 로깅
     */
    public void logBackendResponse(
            HttpServletRequest request,
            HttpServletResponse response,
            String responseBody,
            long duration
    ) {
        if (!enableResponseLogging) {
            return;
        }

        String path = request.getRequestURI();
        if (isExcludedPath(path)) {
            return;
        }

        try {
            Map<String, Object> logData = new HashMap<>();
            logData.put("timestamp", java.time.LocalDateTime.now().toString());
            logData.put("type", "BACKEND_RESPONSE");
            logData.put("status", response.getStatus());
            logData.put("method", request.getMethod());
            logData.put("uri", path);
            logData.put("clientIp", getClientIp(request));
            logData.put("duration", duration + "ms");
            
            // Request ID 추가 (MDC에서 읽기)
            String requestId = MDC.get("requestId");
            if (requestId != null) {
                logData.put("requestId", requestId);
            }
            
            // 응답 본문 파싱 및 마스킹
            if (responseBody != null && !responseBody.isEmpty()) {
                try {
                    Object bodyObj = objectMapper.readValue(responseBody, Object.class);
                    logData.put("body", sanitizeData(bodyObj));
                } catch (Exception e) {
                    logData.put("body", "[Unable to parse JSON]");
                }
            }

            log.info("API Response: {}", objectMapper.writeValueAsString(logData));
        } catch (Exception e) {
            log.warn("Failed to log response: {}", e.getMessage());
        }
    }
}
