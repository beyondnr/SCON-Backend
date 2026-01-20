package vibe.scon.scon_backend.config;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * RestTemplate 설정.
 * 
 * <p>GA4 Measurement Protocol API 호출을 위한 RestTemplate Bean을 설정합니다.</p>
 * 
 * <h3>주요 설정:</h3>
 * <ul>
 *   <li>Connection Pool: 최대 100개, 라우트당 20개</li>
 *   <li>타임아웃: 연결/요청/응답 모두 5초</li>
 *   <li>Idle Connection 정리: 30초</li>
 * </ul>
 * 
 * <h3>요구사항 추적:</h3>
 * <ul>
 *   <li>{@code BE_GA4.md Phase 1.2}: RestTemplate Bean 생성</li>
 * </ul>
 * 
 * @see RestTemplate
 */
@Configuration
public class RestTemplateConfig {
    
    /**
     * RestTemplate Bean 생성.
     * 
     * <p>Apache HttpClient 5.x를 사용하여 Connection Pool과 타임아웃을 설정합니다.</p>
     * 
     * @return RestTemplate 인스턴스
     */
    @Bean
    public RestTemplate restTemplate() {
        // Connection Pool 설정
        PoolingHttpClientConnectionManager connectionManager = 
            new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(100);
        connectionManager.setDefaultMaxPerRoute(20);
        
        // HTTP Client 생성
        CloseableHttpClient httpClient = HttpClients.custom()
            .setConnectionManager(connectionManager)
            .evictIdleConnections(Timeout.of(30, TimeUnit.SECONDS))
            .evictExpiredConnections()
            .build();
        
        // Request Factory 설정
        HttpComponentsClientHttpRequestFactory factory = 
            new HttpComponentsClientHttpRequestFactory(httpClient);
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setConnectionRequestTimeout(Duration.ofSeconds(5));
        
        return new RestTemplate(factory);
    }
}
