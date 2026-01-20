package vibe.scon.scon_backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Google Analytics 4 (GA4) 이벤트 전송 서비스.
 * 
 * <p>GA4 Measurement Protocol을 사용하여 서버 사이드 이벤트를 전송합니다.</p>
 * 
 * <h3>주요 기능:</h3>
 * <ul>
 *   <li>비동기 이벤트 전송 (@Async 사용)</li>
 *   <li>에러 발생 시에도 비즈니스 로직에 영향 없음</li>
 *   <li>MDC 전파를 통한 로그 추적 지원</li>
 * </ul>
 * 
 * <h3>요구사항 추적:</h3>
 * <ul>
 *   <li>{@code BE_GA4.md Phase 1.4}: AnalyticsService 생성</li>
 * </ul>
 * 
 * @see <a href="https://developers.google.com/analytics/devguides/collection/protocol/ga4">GA4 Measurement Protocol</a>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {
    
    private final RestTemplate restTemplate;
    
    @Value("${analytics.ga4.enabled:true}")
    private boolean enabled;
    
    @Value("${analytics.ga4.measurement-id:}")
    private String measurementId;
    
    @Value("${analytics.ga4.api-secret:}")
    private String apiSecret;
    
    private static final String GA4_ENDPOINT = "https://www.google-analytics.com/mp/collect";
    
    /**
     * GA4 이벤트 전송 (비동기).
     * 
     * <p>비동기 작업에서 실행되지만, TaskDecorator를 통해 MDC가 전파되므로
     * 로그에 requestId가 포함됩니다.</p>
     * 
     * <p>GA4 전송 실패 시에도 비즈니스 로직에 영향을 주지 않도록
     * 예외를 잡아 로그만 기록합니다.</p>
     * 
     * @param userId 사용자 ID (필수)
     * @param sessionId 세션 ID (필수, Request ID 사용)
     * @param eventName 이벤트명 (필수)
     * @param parameters 추가 매개변수
     */
    @Async("taskExecutor")
    public void logEvent(String userId, String sessionId, String eventName, Map<String, Object> parameters) {
        // GA4 비활성화 시 스킵
        if (!enabled || measurementId == null || measurementId.isEmpty() || 
            apiSecret == null || apiSecret.isEmpty()) {
            log.debug("GA4 is disabled or not configured. Skipping event: {}", eventName);
            return;
        }
        
        try {
            // 공통 매개변수 추가
            Map<String, Object> enrichedParams = new HashMap<>(parameters);
            enrichedParams.put("user_id", userId);
            enrichedParams.put("session_id", sessionId);
            
            // GA4 Measurement Protocol URL
            String url = String.format(
                "%s?measurement_id=%s&api_secret=%s",
                GA4_ENDPOINT, measurementId, apiSecret
            );
            
            // 페이로드 구성
            Map<String, Object> payload = new HashMap<>();
            payload.put("client_id", userId); // GA4에서 사용자 식별
            payload.put("user_id", userId);   // 커스텀 사용자 ID
            
            Map<String, Object> event = new HashMap<>();
            event.put("name", eventName);
            event.put("params", enrichedParams);
            
            payload.put("events", List.of(event));
            
            // HTTP POST 요청 전송 (비동기)
            restTemplate.postForObject(url, payload, String.class);
            
            // MDC의 requestId가 로그에 포함됨 (TaskDecorator를 통해 전파됨)
            log.debug("GA4 event sent successfully: event={}, user_id={}", eventName, userId);
            
        } catch (Exception e) {
            // 분석 이벤트 전송 실패가 비즈니스 로직에 영향을 주지 않도록 로그만 기록
            log.error("Failed to send GA4 event: event={}, user_id={}, error={}", 
                eventName, userId, e.getMessage(), e);
        }
    }
}
