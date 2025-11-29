package vibe.scon.scon_backend.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JwtTokenProvider 단위 테스트.
 * 
 * <h3>테스트 케이스 추적:</h3>
 * <ul>
 *   <li>TC-AUTH-001: 회원가입 성공 (JWT 발급 부분)</li>
 *   <li>TC-AUTH-002: 비밀번호 BCrypt 해시 검증</li>
 *   <li>TC-AUTH-004: 로그인 성공 + JWT 발급</li>
 *   <li>TC-AUTH-007: 만료된 Access Token 처리</li>
 *   <li>TC-AUTH-008: 유효하지 않은 토큰 처리</li>
 * </ul>
 */
@DisplayName("JwtTokenProvider 단위 테스트")
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        // 테스트용 JWT 설정: Access 30분, Refresh 7일
        jwtTokenProvider = new JwtTokenProvider(
                "test-jwt-secret-key-for-unit-testing",
                1800000L,   // 30분
                604800000L  // 7일
        );
    }

    @Test
    @DisplayName("TC-AUTH-004: Access Token 생성 성공")
    void generateAccessToken_success() {
        // Given
        Long ownerId = 1L;
        String email = "test@example.com";

        // When
        String token = jwtTokenProvider.generateAccessToken(ownerId, email);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT는 3개의 부분으로 구성
    }

    @Test
    @DisplayName("TC-AUTH-004: Refresh Token 생성 성공")
    void generateRefreshToken_success() {
        // Given
        Long ownerId = 1L;

        // When
        String token = jwtTokenProvider.generateRefreshToken(ownerId);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("TC-AUTH-004: 토큰에서 Owner ID 추출 성공")
    void getOwnerIdFromToken_success() {
        // Given
        Long ownerId = 123L;
        String token = jwtTokenProvider.generateAccessToken(ownerId, "test@example.com");

        // When
        Long extractedOwnerId = jwtTokenProvider.getOwnerIdFromToken(token);

        // Then
        assertThat(extractedOwnerId).isEqualTo(ownerId);
    }

    @Test
    @DisplayName("TC-AUTH-004: 토큰에서 이메일 추출 성공")
    void getEmailFromToken_success() {
        // Given
        String email = "test@example.com";
        String token = jwtTokenProvider.generateAccessToken(1L, email);

        // When
        String extractedEmail = jwtTokenProvider.getEmailFromToken(token);

        // Then
        assertThat(extractedEmail).isEqualTo(email);
    }

    @Test
    @DisplayName("유효한 토큰 검증 성공")
    void validateToken_validToken_returnsTrue() {
        // Given
        String token = jwtTokenProvider.generateAccessToken(1L, "test@example.com");

        // When
        boolean isValid = jwtTokenProvider.validateToken(token);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("TC-AUTH-008: 잘못된 형식의 토큰 검증 실패")
    void validateToken_invalidToken_returnsFalse() {
        // Given
        String invalidToken = "invalid.token.here";

        // When
        boolean isValid = jwtTokenProvider.validateToken(invalidToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("TC-AUTH-008: 빈 토큰 검증 실패")
    void validateToken_emptyToken_returnsFalse() {
        // Given
        String emptyToken = "";

        // When
        boolean isValid = jwtTokenProvider.validateToken(emptyToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("TC-AUTH-007: 만료된 토큰 검증 실패")
    void validateToken_expiredToken_returnsFalse() {
        // Given - 만료 시간이 매우 짧은 토큰 제공자
        JwtTokenProvider shortLivedProvider = new JwtTokenProvider(
                "test-jwt-secret-key",
                1L,  // 1ms (즉시 만료)
                1L
        );
        String token = shortLivedProvider.generateAccessToken(1L, "test@example.com");

        // When - 토큰 생성 직후 약간의 지연
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Then
        boolean isValid = shortLivedProvider.validateToken(token);
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Access Token 타입 확인")
    void isAccessToken_accessToken_returnsTrue() {
        // Given
        String token = jwtTokenProvider.generateAccessToken(1L, "test@example.com");

        // When & Then
        assertThat(jwtTokenProvider.isAccessToken(token)).isTrue();
        assertThat(jwtTokenProvider.isRefreshToken(token)).isFalse();
    }

    @Test
    @DisplayName("Refresh Token 타입 확인")
    void isRefreshToken_refreshToken_returnsTrue() {
        // Given
        String token = jwtTokenProvider.generateRefreshToken(1L);

        // When & Then
        assertThat(jwtTokenProvider.isRefreshToken(token)).isTrue();
        assertThat(jwtTokenProvider.isAccessToken(token)).isFalse();
    }

    @Test
    @DisplayName("토큰 타입 추출 - Access Token")
    void getTokenType_accessToken_returnsAccess() {
        // Given
        String token = jwtTokenProvider.generateAccessToken(1L, "test@example.com");

        // When
        String type = jwtTokenProvider.getTokenType(token);

        // Then
        assertThat(type).isEqualTo("access");
    }

    @Test
    @DisplayName("토큰 타입 추출 - Refresh Token")
    void getTokenType_refreshToken_returnsRefresh() {
        // Given
        String token = jwtTokenProvider.generateRefreshToken(1L);

        // When
        String type = jwtTokenProvider.getTokenType(token);

        // Then
        assertThat(type).isEqualTo("refresh");
    }

    @Test
    @DisplayName("다른 비밀키로 서명된 토큰 검증 실패")
    void validateToken_differentSecretKey_returnsFalse() {
        // Given - 다른 비밀키로 생성된 토큰
        JwtTokenProvider otherProvider = new JwtTokenProvider(
                "completely-different-secret-key-for-testing",
                1800000L,
                604800000L
        );
        String tokenFromOtherProvider = otherProvider.generateAccessToken(1L, "test@example.com");

        // When - 원래 provider로 검증 (validateToken은 예외를 catch하고 false 반환)
        boolean isValid = jwtTokenProvider.validateToken(tokenFromOtherProvider);

        // Then - 다른 키로 서명된 토큰은 검증 실패해야 함
        assertThat(isValid).isFalse();
    }
}
