/**
 * 애플리케이션 설정 프로퍼티 클래스 패키지.
 * 
 * <p>application.yml의 커스텀 프로퍼티를 타입 안전하게 바인딩하는 클래스들을 포함합니다.</p>
 * 
 * <h2>주요 클래스</h2>
 * <ul>
 *   <li>{@link vibe.scon.scon_backend.config.properties.AppProperties} - 메인 설정 클래스</li>
 * </ul>
 * 
 * <h2>환경변수 매핑</h2>
 * <pre>
 * app.jwt.secret         → JWT_SECRET_KEY
 * app.encryption.key     → ENCRYPTION_KEY
 * app.ai.openai.api-key  → OPENAI_API_KEY
 * app.ai.gemini.api-key  → GEMINI_API_KEY
 * </pre>
 * 
 * @see vibe.scon.scon_backend.config.EnvironmentValidator
 */
package vibe.scon.scon_backend.config.properties;
