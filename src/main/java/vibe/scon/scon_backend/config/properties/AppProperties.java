package vibe.scon.scon_backend.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

/**
 * 애플리케이션 커스텀 설정 프로퍼티.
 * 
 * <p>application.yml의 'app' 프리픽스 하위 설정을 바인딩합니다.</p>
 * 
 * <pre>
 * app:
 *   jwt:
 *     secret: ...
 *   ai:
 *     openai:
 *       api-key: ...
 * </pre>
 * 
 * @see JwtProperties
 * @see EncryptionProperties
 * @see AiProperties
 */
@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppProperties {

    /**
     * JWT 인증 관련 설정.
     */
    private JwtProperties jwt = new JwtProperties();

    /**
     * PII 암호화 관련 설정.
     */
    private EncryptionProperties encryption = new EncryptionProperties();

    /**
     * AI 모델 API 설정.
     */
    private AiProperties ai = new AiProperties();

    /**
     * 알림 서비스 설정.
     */
    private NotificationProperties notification = new NotificationProperties();

    /**
     * 스토리지 설정.
     */
    private StorageProperties storage = new StorageProperties();

    // =========================================================================
    // Nested Configuration Classes
    // =========================================================================

    /**
     * JWT 설정 프로퍼티.
     */
    @Getter
    @Setter
    public static class JwtProperties {
        /**
         * JWT 서명에 사용할 비밀 키 (최소 32자).
         */
        private String secret;

        /**
         * Access Token 만료 시간 (밀리초). 기본값: 30분.
         */
        private long accessExpiration = 1800000L;

        /**
         * Refresh Token 만료 시간 (밀리초). 기본값: 7일.
         */
        private long refreshExpiration = 604800000L;
    }

    /**
     * 암호화 설정 프로퍼티.
     */
    @Getter
    @Setter
    public static class EncryptionProperties {
        /**
         * AES 암호화 키 (32바이트).
         */
        private String key;

        /**
         * 암호화 알고리즘. 기본값: AES/GCM/NoPadding.
         */
        private String algorithm = "AES/GCM/NoPadding";
    }

    /**
     * AI 모델 API 설정 프로퍼티.
     */
    @Getter
    @Setter
    public static class AiProperties {
        /**
         * OpenAI API 설정.
         */
        private OpenAiProperties openai = new OpenAiProperties();

        /**
         * Google Gemini API 설정.
         */
        private GeminiProperties gemini = new GeminiProperties();

        /**
         * LangChain 마이크로서비스 설정.
         */
        private LangChainProperties langchain = new LangChainProperties();
    }

    /**
     * OpenAI API 설정.
     */
    @Getter
    @Setter
    public static class OpenAiProperties {
        /**
         * OpenAI API 키.
         */
        private String apiKey;

        /**
         * 사용할 모델. 기본값: gpt-4.
         */
        private String model = "gpt-4";

        /**
         * API 베이스 URL.
         */
        private String baseUrl = "https://api.openai.com/v1";

        /**
         * 최대 토큰 수.
         */
        private int maxTokens = 2000;

        /**
         * Temperature (0.0 ~ 2.0).
         */
        private double temperature = 0.7;

        /**
         * API 키가 설정되어 있는지 확인.
         */
        public boolean isConfigured() {
            return apiKey != null && !apiKey.isBlank() && !apiKey.startsWith("sk-your");
        }
    }

    /**
     * Google Gemini API 설정.
     */
    @Getter
    @Setter
    public static class GeminiProperties {
        /**
         * Gemini API 키.
         */
        private String apiKey;

        /**
         * 사용할 모델. 기본값: gemini-pro.
         */
        private String model = "gemini-pro";

        /**
         * API 키가 설정되어 있는지 확인.
         */
        public boolean isConfigured() {
            return apiKey != null && !apiKey.isBlank() && !apiKey.startsWith("your");
        }
    }

    /**
     * LangChain 마이크로서비스 설정.
     */
    @Getter
    @Setter
    public static class LangChainProperties {
        /**
         * 서비스 URL.
         */
        private String serviceUrl = "http://localhost:8000";

        /**
         * 내부 API 키.
         */
        private String apiKey;

        /**
         * 서비스가 설정되어 있는지 확인.
         */
        public boolean isConfigured() {
            return serviceUrl != null && !serviceUrl.isBlank();
        }
    }

    /**
     * 알림 서비스 설정.
     */
    @Getter
    @Setter
    public static class NotificationProperties {
        /**
         * Mock 모드 활성화 (로컬 개발용).
         */
        private boolean mockEnabled = false;

        /**
         * 카카오 알림톡 설정.
         */
        private KakaoProperties kakao = new KakaoProperties();

        /**
         * SMS 설정.
         */
        private SmsProperties sms = new SmsProperties();
    }

    /**
     * 카카오 알림톡 설정.
     */
    @Getter
    @Setter
    public static class KakaoProperties {
        private String apiKey;
        private String senderKey;

        public boolean isConfigured() {
            return apiKey != null && !apiKey.isBlank();
        }
    }

    /**
     * SMS 설정.
     */
    @Getter
    @Setter
    public static class SmsProperties {
        private String apiKey;
        private String senderNumber;

        public boolean isConfigured() {
            return apiKey != null && !apiKey.isBlank();
        }
    }

    /**
     * 스토리지 설정.
     */
    @Getter
    @Setter
    public static class StorageProperties {
        /**
         * 스토리지 타입: local, s3.
         */
        private String type = "local";

        /**
         * 로컬 스토리지 설정.
         */
        private LocalStorageProperties local = new LocalStorageProperties();

        /**
         * AWS S3 설정.
         */
        private AwsProperties aws = new AwsProperties();
    }

    /**
     * 로컬 스토리지 설정.
     */
    @Getter
    @Setter
    public static class LocalStorageProperties {
        private String basePath = "./storage";
    }

    /**
     * AWS 설정.
     */
    @Getter
    @Setter
    public static class AwsProperties {
        private String accessKey;
        private String secretKey;
        private String region = "ap-northeast-2";
        private String s3Bucket;

        public boolean isConfigured() {
            return accessKey != null && !accessKey.isBlank() 
                && secretKey != null && !secretKey.isBlank();
        }
    }
}
