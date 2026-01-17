package vibe.scon.scon_backend.config;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import vibe.scon.scon_backend.config.filter.JwtAuthenticationFilter;
import vibe.scon.scon_backend.config.filter.RateLimitingFilter;
import vibe.scon.scon_backend.config.filter.RequestIdTrackingFilter;
import vibe.scon.scon_backend.config.filter.RequestResponseLoggingFilter;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security 설정 클래스.
 * 
 * <p>JWT 기반 무상태(Stateless) 인증을 설정합니다.
 * CSRF 비활성화, 세션 미사용, JWT 필터 적용을 구성합니다.</p>
 * 
 * <h3>요구사항 추적 (Traceability):</h3>
 * <ul>
 *   <li>{@code REQ-FUNC-001} - 3단계 온보딩 마법사 (인증/인가)</li>
 *   <li>{@code REQ-NF-008} - 전송 구간 암호화 (TLS 1.3)</li>
 *   <li>{@code Issue-003 §7.2} - 비밀번호 해시 (BCrypt)</li>
 * </ul>
 * 
 * <h3>보안 설정:</h3>
 * <ul>
 *   <li>CSRF: 비활성화 (REST API이므로)</li>
 *   <li>세션: STATELESS (JWT 사용)</li>
 *   <li>비밀번호 인코더: BCrypt (cost factor 12)</li>
 * </ul>
 * 
 * <h3>공개 엔드포인트:</h3>
 * <ul>
 *   <li>{@code /api/v1/auth/signup} - 회원가입</li>
 *   <li>{@code /api/v1/auth/login} - 로그인</li>
 *   <li>{@code /api/v1/auth/refresh} - 토큰 갱신</li>
 *   <li>{@code /api/v1/auth/logout} - 로그아웃 (Refresh Token으로 인증)</li>
 * </ul>
 * 
 * @see JwtAuthenticationFilter
 * @see <a href="tasks/github-issues/issue-003-REQ-FUNC-001-003.md">Issue-003</a>
 * @see <a href="../../SCON-Update-Plan/POC-BE-FUNC-003-logout.md">POC-BE-FUNC-003</a>
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitingFilter rateLimitingFilter;
    private final RequestIdTrackingFilter requestIdTrackingFilter;
    private final RequestResponseLoggingFilter requestResponseLoggingFilter;
    private final Environment environment;

    /**
     * Security Filter Chain 설정 (개발 환경).
     * 
     * @param http HttpSecurity 객체
     * @return 구성된 SecurityFilterChain
     * @throws Exception 설정 실패 시
     */
    @Bean
    @Profile({"dev", "local", "!prod"})
    public SecurityFilterChain devSecurityFilterChain(HttpSecurity http) throws Exception {
        return buildSecurityFilterChain(http);
    }

    /**
     * Security Filter Chain 설정 (프로덕션 환경).
     * 
     * @param http HttpSecurity 객체
     * @return 구성된 SecurityFilterChain
     * @throws Exception 설정 실패 시
     */
    @Bean
    @Profile("prod")
    public SecurityFilterChain prodSecurityFilterChain(HttpSecurity http) throws Exception {
        return buildSecurityFilterChain(http);
    }

    /**
     * Security Filter Chain 빌더 메서드.
     * 
     * <p>공통 Security 설정을 구성합니다.</p>
     * 
     * @param http HttpSecurity 객체
     * @return 구성된 SecurityFilterChain
     * @throws Exception 설정 실패 시
     */
    private SecurityFilterChain buildSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                // CORS 설정 적용 (프론트엔드 연동을 위해 필수)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                
                // CSRF 비활성화 (REST API는 상태를 저장하지 않으므로)
                .csrf(AbstractHttpConfigurer::disable)
                
                // 세션 관리: STATELESS (JWT 사용)
                .sessionManagement(session -> 
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                
                // HTTP 요청 인가 설정
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(
                            "/api/v1/auth/signup",
                            "/api/v1/auth/login",
                            "/api/v1/auth/refresh",
                            "/api/v1/auth/logout",
                            "/health",
                            "/api/health",
                            "/api/v1/health"
                    ).permitAll();
                    
                    // Swagger/OpenAPI (추후 추가 시)
                    auth.requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll();
                    
                    // 그 외 모든 요청은 인증 필요
                    auth.anyRequest().authenticated();
                })
                
                // Security 헤더 설정 (POC-BE-SEC-002)
                .headers(headers -> headers
                        // Clickjacking 방어: DENY (프로덕션), sameOrigin (개발 환경)
                        .frameOptions(frameOptions -> {
                            String activeProfile = environment.getProperty("spring.profiles.active", "dev");
                            boolean isProduction = "prod".equals(activeProfile) || "production".equals(activeProfile);
                            if (isProduction) {
                                frameOptions.deny();
                            } else {
                                frameOptions.sameOrigin(); // 개발 환경
                            }
                        })
                        // MIME 스니핑 방어
                        .contentTypeOptions(contentTypeOptions -> contentTypeOptions.disable())
                        // HSTS (HTTPS 강제)
                        .httpStrictTransportSecurity(hsts -> hsts
                                .maxAgeInSeconds(31536000) // 1년
                        )
                        // 추가 Security 헤더
                        .addHeaderWriter(new StaticHeadersWriter(
                                "X-Content-Type-Options", "nosniff",
                                "X-Frame-Options", "DENY",
                                "X-XSS-Protection", "1; mode=block",
                                "Content-Security-Policy", "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'"
                        ))
                )
                
                // 필터 등록 순서:
                // 1. RateLimitingFilter (가장 먼저 실행 - POC-BE-SEC-002)
                // 2. RequestIdTrackingFilter (Request ID 추적 - 신규 추가)
                // 3. JwtAuthenticationFilter (JWT 토큰 검증)
                // 4. RequestResponseLoggingFilter (요청/응답 로깅)
                // 5. UsernamePasswordAuthenticationFilter (Spring Security 기본 필터)
                
                // Rate Limiting 필터 추가 (가장 먼저 실행 - POC-BE-SEC-002)
                // RateLimitingFilter 내부에서 테스트 프로파일 확인하여 비활성화
                .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
                
                // Request ID Tracking 필터 추가 (RateLimitingFilter와 JwtAuthenticationFilter 사이)
                .addFilterBefore(requestIdTrackingFilter, JwtAuthenticationFilter.class)
                
                // JWT 필터 추가 (UsernamePasswordAuthenticationFilter 이전에 실행)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                
                // Request/Response Logging 필터 추가 (JwtAuthenticationFilter 이후 실행)
                .addFilterAfter(requestResponseLoggingFilter, JwtAuthenticationFilter.class)
                
                // 예외 처리: 인증 실패 시 401 반환
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"status\":401,\"message\":\"인증이 필요합니다\"}");
                        })
                );

        return http.build();
    }

    /**
     * Password Encoder Bean.
     * 
     * <p>비밀번호 해싱에 BCrypt 알고리즘을 사용합니다.
     * Cost factor 12는 보안과 성능의 균형점입니다.</p>
     * 
     * @return BCryptPasswordEncoder 인스턴스 (strength: 12)
     * @see <a href="tasks/github-issues/issue-003-REQ-FUNC-001-003.md">Issue-003 §7.2</a>
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt with cost factor 12 (Issue-003 §7.2 스펙)
        return new BCryptPasswordEncoder(12);
    }

    /**
     * CORS Configuration Source Bean.
     * 
     * <p>프론트엔드 개발 서버 및 프로덕션 도메인에서의 API 호출을 허용하기 위한 CORS 설정입니다.</p>
     * 
     * <h3>환경별 설정:</h3>
     * <ul>
     *   <li>개발 환경: 기본값으로 localhost 포트 허용</li>
     *   <li>프로덕션 환경: 환경 변수 {@code CORS_ALLOWED_ORIGINS}로 설정된 도메인만 허용</li>
     * </ul>
     * 
     * <h3>요구사항 추적 (Traceability):</h3>
     * <ul>
     *   <li>{@code POC-BE-SEC-002} - 백엔드 보안 강화 (CORS 설정)</li>
     * </ul>
     * 
     * @return CorsConfigurationSource 인스턴스
     * @throws IllegalStateException 프로덕션 환경에서 CORS_ALLOWED_ORIGINS가 설정되지 않은 경우
     * @see <a href="../../SCON-Update-Plan/POC-BE-SEC-002.md">POC-BE-SEC-002</a>
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 환경 변수에서 CORS 허용 Origin 읽기
        String allowedOriginsEnv = environment.getProperty("app.cors.allowed-origins");
        String activeProfile = environment.getProperty("spring.profiles.active", "dev");
        boolean isProduction = "prod".equals(activeProfile) || "production".equals(activeProfile);
        
        if (allowedOriginsEnv != null && !allowedOriginsEnv.trim().isEmpty()) {
            // 환경 변수가 설정된 경우: 특정 Origin만 허용
            List<String> origins = Arrays.asList(allowedOriginsEnv.split(","));
            configuration.setAllowedOrigins(origins.stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList());
        } else {
            // 환경 변수가 설정되지 않은 경우
            if (isProduction) {
                throw new IllegalStateException(
                    "프로덕션 환경에서는 app.cors.allowed-origins 환경 변수를 반드시 설정해야 합니다. " +
                    "예: CORS_ALLOWED_ORIGINS=https://lawfulshift.com,https://www.lawfulshift.com"
                );
            }
            // 개발 환경 기본값: localhost 포트 허용
            configuration.setAllowedOriginPatterns(List.of(
                    "http://localhost:3000",
                    "http://localhost:5173",
                    "http://localhost:5174",
                    "http://localhost:9002"
            ));
        }
        
        // 허용할 HTTP 메서드
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));
        
        // 특정 헤더만 허용 (와일드카드 제거 - POC-BE-SEC-002)
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "X-Request-ID"  // Request ID 추적용 헤더
        ));
        
        // 자격 증명(쿠키, Authorization 헤더) 허용
        configuration.setAllowCredentials(true);
        
        // Preflight 요청 캐시 시간 (1시간)
        configuration.setMaxAge(3600L);
        
        // 노출할 응답 헤더
        configuration.setExposedHeaders(List.of(
                "Authorization",
                "X-Request-ID"  // Request ID 추적용 헤더
        ));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
