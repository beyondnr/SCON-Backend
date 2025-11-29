package vibe.scon.scon_backend.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * EncryptionUtil 단위 테스트.
 * 
 * <h3>테스트 케이스 추적:</h3>
 * <ul>
 *   <li>TC-EMP-002: PII 암호화 저장 검증 (REQ-NF-007)</li>
 *   <li>TC-EMP-003: PII 복호화 응답 검증</li>
 *   <li>TC-NFR-002: AES-256 암호화 저장 검증</li>
 * </ul>
 */
@DisplayName("EncryptionUtil 단위 테스트")
class EncryptionUtilTest {

    private EncryptionUtil encryptionUtil;

    @BeforeEach
    void setUp() {
        // 테스트용 암호화 키로 초기화
        encryptionUtil = new EncryptionUtil("test-encryption-key-for-unit-test");
    }

    @Test
    @DisplayName("TC-EMP-002: 문자열 암호화 성공")
    void encrypt_success() {
        // Given
        String plainText = "010-1234-5678";

        // When
        String encrypted = encryptionUtil.encrypt(plainText);

        // Then
        assertThat(encrypted).isNotNull();
        assertThat(encrypted).isNotEqualTo(plainText);
        assertThat(encrypted.length()).isGreaterThan(plainText.length());
    }

    @Test
    @DisplayName("TC-EMP-003: 암호화된 문자열 복호화 성공")
    void decrypt_success() {
        // Given
        String plainText = "010-1234-5678";
        String encrypted = encryptionUtil.encrypt(plainText);

        // When
        String decrypted = encryptionUtil.decrypt(encrypted);

        // Then
        assertThat(decrypted).isEqualTo(plainText);
    }

    @Test
    @DisplayName("TC-NFR-002: 동일한 평문도 매번 다른 암호문 생성 (IV 랜덤)")
    void encrypt_differentCiphertextEachTime() {
        // Given
        String plainText = "010-1234-5678";

        // When
        String encrypted1 = encryptionUtil.encrypt(plainText);
        String encrypted2 = encryptionUtil.encrypt(plainText);

        // Then - 같은 평문이지만 IV가 달라서 암호문이 다름
        assertThat(encrypted1).isNotEqualTo(encrypted2);
        
        // 하지만 둘 다 복호화하면 같은 평문
        assertThat(encryptionUtil.decrypt(encrypted1)).isEqualTo(plainText);
        assertThat(encryptionUtil.decrypt(encrypted2)).isEqualTo(plainText);
    }

    @Test
    @DisplayName("null 입력 시 null 반환")
    void encrypt_nullInput_returnsNull() {
        // When & Then
        assertThat(encryptionUtil.encrypt(null)).isNull();
        assertThat(encryptionUtil.decrypt(null)).isNull();
    }

    @Test
    @DisplayName("빈 문자열 입력 시 빈 문자열 반환")
    void encrypt_emptyInput_returnsEmpty() {
        // When & Then
        assertThat(encryptionUtil.encrypt("")).isEmpty();
        assertThat(encryptionUtil.decrypt("")).isEmpty();
    }

    @Test
    @DisplayName("암호화 여부 확인 - 암호화된 데이터")
    void isEncrypted_encryptedData_returnsTrue() {
        // Given
        String encrypted = encryptionUtil.encrypt("test-data");

        // When & Then
        assertThat(encryptionUtil.isEncrypted(encrypted)).isTrue();
    }

    @Test
    @DisplayName("암호화 여부 확인 - 일반 텍스트")
    void isEncrypted_plainText_returnsFalse() {
        // Given
        String plainText = "010-1234-5678";

        // When & Then
        assertThat(encryptionUtil.isEncrypted(plainText)).isFalse();
    }

    @Test
    @DisplayName("잘못된 암호문 복호화 시 예외 발생")
    void decrypt_invalidCiphertext_throwsException() {
        // Given
        String invalidCiphertext = "invalid-base64-data!!!";

        // When & Then
        assertThatThrownBy(() -> encryptionUtil.decrypt(invalidCiphertext))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("한글 문자열 암호화/복호화 성공")
    void encryptDecrypt_koreanText_success() {
        // Given
        String koreanText = "홍길동";

        // When
        String encrypted = encryptionUtil.encrypt(koreanText);
        String decrypted = encryptionUtil.decrypt(encrypted);

        // Then
        assertThat(decrypted).isEqualTo(koreanText);
    }

    @Test
    @DisplayName("긴 문자열 암호화/복호화 성공")
    void encryptDecrypt_longText_success() {
        // Given
        String longText = "This is a very long text that should be encrypted and decrypted correctly. ".repeat(10);

        // When
        String encrypted = encryptionUtil.encrypt(longText);
        String decrypted = encryptionUtil.decrypt(encrypted);

        // Then
        assertThat(decrypted).isEqualTo(longText);
    }

    @Test
    @DisplayName("특수문자 포함 문자열 암호화/복호화 성공")
    void encryptDecrypt_specialCharacters_success() {
        // Given
        String specialText = "!@#$%^&*()_+-=[]{}|;':\",./<>?";

        // When
        String encrypted = encryptionUtil.encrypt(specialText);
        String decrypted = encryptionUtil.decrypt(encrypted);

        // Then
        assertThat(decrypted).isEqualTo(specialText);
    }
}
