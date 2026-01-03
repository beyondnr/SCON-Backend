package vibe.scon.scon_backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger/OpenAPI 설정 클래스.
 * 
 * <p>API 문서화를 위한 SpringDoc OpenAPI 설정입니다.
 * Swagger UI를 통해 API를 시각적으로 확인하고 테스트할 수 있습니다.</p>
 * 
 * <h3>접근 URL:</h3>
 * <ul>
 *   <li>Swagger UI: http://localhost:8080/swagger-ui/index.html</li>
 *   <li>OpenAPI JSON: http://localhost:8080/v3/api-docs</li>
 * </ul>
 * 
 * @see <a href="https://springdoc.org/">SpringDoc OpenAPI</a>
 */
@Configuration
public class SwaggerConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    /**
     * OpenAPI 설정 Bean.
     * 
     * <p>API 문서의 기본 정보, 서버 정보, 보안 설정을 구성합니다.</p>
     * 
     * @return OpenAPI 인스턴스
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local Development Server")
                ))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT 토큰을 입력하세요. (Bearer 접두사 없이 토큰만 입력)")
                        ));
    }

    /**
     * API 정보 설정.
     * 
     * @return API 정보 객체
     */
    private Info apiInfo() {
        return new Info()
                .title("SCON Backend API")
                .version("1.0.0")
                .description("""
                        SCON (Shift Control) Backend API Documentation
                        
                        ## 개요
                        SCON은 베이커리 카페 및 중소 사업장을 위한 근무 스케줄 관리 SaaS 솔루션입니다.
                        
                        ## 인증
                        대부분의 API는 JWT 토큰 인증이 필요합니다.
                        1. `/api/v1/auth/signup` 또는 `/api/v1/auth/login`을 통해 토큰을 발급받으세요.
                        2. 우측 상단의 'Authorize' 버튼을 클릭하여 토큰을 입력하세요.
                        3. 이후 인증이 필요한 API를 테스트할 수 있습니다.
                        
                        ## 주요 API 그룹
                        - **Auth**: 회원가입, 로그인, 토큰 갱신
                        - **Store**: 매장 생성 및 관리
                        - **Employee**: 직원 등록 및 조회
                        - **Schedule**: 스케줄 조회
                        """)
                .contact(new Contact()
                        .name("SCON Team")
                        .email("support@scon.io"));
    }
}

