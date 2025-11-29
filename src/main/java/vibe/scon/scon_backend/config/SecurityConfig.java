package vibe.scon.scon_backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import vibe.scon.scon_backend.config.filter.JwtAuthenticationFilter;

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
 * @see JwtAuthenticationFilter
 * @see <a href="tasks/github-issues/issue-003-REQ-FUNC-001-003.md">Issue-003</a>
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * Security Filter Chain 설정.
     * 
     * <p>API 엔드포인트별 접근 권한을 설정합니다.</p>
     * 
     * <ul>
     *   <li>공개 API: /api/v1/auth/signup, /api/v1/auth/login, /health</li>
     *   <li>인증 필요: 그 외 모든 /api/** 엔드포인트</li>
     * </ul>
     * 
     * @param http HttpSecurity 객체
     * @return 구성된 SecurityFilterChain
     * @throws Exception 설정 실패 시
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 비활성화 (REST API는 상태를 저장하지 않으므로)
                .csrf(AbstractHttpConfigurer::disable)
                
                // 세션 관리: STATELESS (JWT 사용)
                .sessionManagement(session -> 
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                
                // HTTP 요청 인가 설정
                .authorizeHttpRequests(auth -> auth
                        // 공개 엔드포인트 (인증 불필요)
                        .requestMatchers(
                                "/api/v1/auth/signup",
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh",
                                "/health",
                                "/api/health"
                        ).permitAll()
                        
                        // H2 콘솔 (개발용)
                        .requestMatchers("/h2-console/**").permitAll()
                        
                        // Swagger/OpenAPI (추후 추가 시)
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        
                        // 그 외 모든 요청은 인증 필요
                        .anyRequest().authenticated()
                )
                
                // H2 콘솔 iframe 허용 (개발용)
                .headers(headers -> headers
                        .frameOptions(frameOptions -> frameOptions.sameOrigin()))
                
                // JWT 필터 추가 (UsernamePasswordAuthenticationFilter 이전에 실행)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

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
}
