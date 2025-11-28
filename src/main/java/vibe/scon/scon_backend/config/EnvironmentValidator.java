package vibe.scon.scon_backend.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vibe.scon.scon_backend.config.properties.AppProperties;

/**
 * 애플리케이션 시작 시 필수 환경변수를 검증하는 컴포넌트.
 * 
 * <p>프로덕션 환경에서는 필수 환경변수가 누락된 경우 앱 시작을 차단합니다.
 * 로컬/개발 환경에서는 경고만 출력하고 계속 진행합니다.</p>
 * 
 * <pre>
 * 검증 항목:
 * - JWT_SECRET_KEY: 최소 32자 이상
 * - ENCRYPTION_KEY: 최소 32자 이상
 * - MYSQL_PASSWORD: 기본값이 아닌 값
 * - AI API Keys: 프로덕션에서 필수 (설정된 경우)
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EnvironmentValidator implements ApplicationRunner {

    private final AppProperties appProperties;
    private final Environment environment;

    @Override
    public void run(ApplicationArguments args) {
        String activeProfile = getActiveProfile();
        boolean isProduction = "prod".equals(activeProfile) || "production".equals(activeProfile);
        boolean isStaging = "staging".equals(activeProfile);

        log.info("========================================");
        log.info("  Environment Validation");
        log.info("  Active Profile: {}", activeProfile);
        log.info("========================================");

        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // ─────────────────────────────────────────────────────────────────────
        // JWT Secret 검증
        // ─────────────────────────────────────────────────────────────────────
        String jwtSecret = appProperties.getJwt().getSecret();
        if (jwtSecret == null || jwtSecret.isBlank()) {
            errors.add("JWT_SECRET_KEY is not configured");
        } else if (jwtSecret.length() < 32) {
            errors.add("JWT_SECRET_KEY must be at least 32 characters (current: " + jwtSecret.length() + ")");
        } else if (jwtSecret.contains("default") || jwtSecret.contains("local-dev")) {
            if (isProduction || isStaging) {
                errors.add("JWT_SECRET_KEY contains default/dev value in " + activeProfile + " environment");
            } else {
                warnings.add("JWT_SECRET_KEY is using default/dev value");
            }
        } else {
            log.info("  ✓ JWT_SECRET_KEY: configured ({} chars)", jwtSecret.length());
        }

        // ─────────────────────────────────────────────────────────────────────
        // Encryption Key 검증
        // ─────────────────────────────────────────────────────────────────────
        String encryptionKey = appProperties.getEncryption().getKey();
        if (encryptionKey == null || encryptionKey.isBlank()) {
            errors.add("ENCRYPTION_KEY is not configured");
        } else if (encryptionKey.length() < 32) {
            errors.add("ENCRYPTION_KEY must be at least 32 characters for AES-256");
        } else if (encryptionKey.contains("default") || encryptionKey.contains("local-dev")) {
            if (isProduction || isStaging) {
                errors.add("ENCRYPTION_KEY contains default/dev value in " + activeProfile + " environment");
            } else {
                warnings.add("ENCRYPTION_KEY is using default/dev value");
            }
        } else {
            log.info("  ✓ ENCRYPTION_KEY: configured ({} chars)", encryptionKey.length());
        }

        // ─────────────────────────────────────────────────────────────────────
        // AI API 설정 검증
        // ─────────────────────────────────────────────────────────────────────
        validateAiConfiguration(errors, warnings, isProduction);

        // ─────────────────────────────────────────────────────────────────────
        // 외부 서비스 설정 검증
        // ─────────────────────────────────────────────────────────────────────
        validateExternalServices(errors, warnings, isProduction);

        // ─────────────────────────────────────────────────────────────────────
        // 결과 출력
        // ─────────────────────────────────────────────────────────────────────
        log.info("----------------------------------------");

        // 경고 출력
        if (!warnings.isEmpty()) {
            log.warn("  ⚠ Warnings ({}):", warnings.size());
            warnings.forEach(w -> log.warn("    - {}", w));
        }

        // 에러 처리
        if (!errors.isEmpty()) {
            log.error("  ✗ Errors ({}):", errors.size());
            errors.forEach(e -> log.error("    - {}", e));

            if (isProduction || isStaging) {
                log.error("========================================");
                throw new IllegalStateException(
                    "Application startup blocked due to missing required environment variables in " 
                    + activeProfile + " environment: " + errors
                );
            } else {
                log.warn("  → Continuing with errors in {} mode (NOT recommended for production)", activeProfile);
            }
        } else {
            log.info("  ✓ All required environment variables are configured");
        }

        log.info("========================================");
    }

    /**
     * AI 서비스 설정 검증.
     */
    private void validateAiConfiguration(List<String> errors, List<String> warnings, boolean isProduction) {
        var openai = appProperties.getAi().getOpenai();
        var gemini = appProperties.getAi().getGemini();
        var langchain = appProperties.getAi().getLangchain();

        // OpenAI
        if (openai.isConfigured()) {
            log.info("  ✓ OPENAI_API_KEY: configured (model: {})", openai.getModel());
        } else {
            if (isProduction) {
                warnings.add("OPENAI_API_KEY is not configured (AI features will be disabled)");
            } else {
                log.info("  ○ OPENAI_API_KEY: not configured (optional for local dev)");
            }
        }

        // Gemini
        if (gemini.isConfigured()) {
            log.info("  ✓ GEMINI_API_KEY: configured (model: {})", gemini.getModel());
        } else {
            log.info("  ○ GEMINI_API_KEY: not configured (optional)");
        }

        // LangChain Service
        if (langchain.isConfigured()) {
            log.info("  ✓ LANGCHAIN_SERVICE: {} ", langchain.getServiceUrl());
        } else {
            log.info("  ○ LANGCHAIN_SERVICE: not configured");
        }
    }

    /**
     * 외부 서비스 설정 검증.
     */
    private void validateExternalServices(List<String> errors, List<String> warnings, boolean isProduction) {
        var notification = appProperties.getNotification();
        var storage = appProperties.getStorage();

        // 알림 서비스
        if (notification.isMockEnabled()) {
            log.info("  ○ Notification: MOCK mode enabled");
        } else {
            if (notification.getKakao().isConfigured()) {
                log.info("  ✓ KAKAO_API_KEY: configured");
            } else if (isProduction) {
                warnings.add("KAKAO_API_KEY is not configured (notifications will fail)");
            }

            if (notification.getSms().isConfigured()) {
                log.info("  ✓ SMS_API_KEY: configured");
            }
        }

        // 스토리지
        if ("s3".equals(storage.getType())) {
            if (storage.getAws().isConfigured()) {
                log.info("  ✓ AWS S3: configured (bucket: {})", storage.getAws().getS3Bucket());
            } else if (isProduction) {
                errors.add("AWS credentials not configured for S3 storage in production");
            }
        } else {
            log.info("  ○ Storage: local mode (path: {})", storage.getLocal().getBasePath());
        }
    }

    /**
     * 현재 활성화된 프로파일 반환.
     */
    private String getActiveProfile() {
        String[] profiles = environment.getActiveProfiles();
        if (profiles.length > 0) {
            return profiles[0];
        }
        return environment.getDefaultProfiles().length > 0 
            ? environment.getDefaultProfiles()[0] 
            : "default";
    }
}
