package vibe.scon.scon_backend.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM 암호화/복호화 유틸리티.
 * 
 * <p>개인정보(PII) 암호화를 위한 유틸리티 클래스입니다.
 * 주로 Employee.phone 필드 암호화에 사용됩니다.</p>
 * 
 * <h3>요구사항 추적 (Traceability):</h3>
 * <ul>
 *   <li>{@code REQ-NF-007} - 저장 데이터 암호화 (AES-256)</li>
 *   <li>{@code Issue-003 §7.2} - 암호화 스펙</li>
 *   <li>{@code TC-EMP-002} - PII 암호화 저장 검증</li>
 * </ul>
 * 
 * <h3>암호화 스펙:</h3>
 * <ul>
 *   <li>알고리즘: AES-256-GCM</li>
 *   <li>IV 길이: 12바이트</li>
 *   <li>Tag 길이: 128비트</li>
 *   <li>키 관리: 환경변수 (ENCRYPTION_KEY)</li>
 * </ul>
 * 
 * @see <a href="docs/GPT-SRS_v0.2.md">SRS §4.2 REQ-NF-007</a>
 * @see <a href="tasks/github-issues/issue-003-REQ-FUNC-001-003.md">Issue-003 §7.2</a>
 */
@Slf4j
@Component
public class EncryptionUtil {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;  // 12바이트 IV (권장)
    private static final int GCM_TAG_LENGTH = 128; // 128비트 인증 태그

    private final SecretKey secretKey;

    /**
     * EncryptionUtil 생성자.
     * 
     * <p>입력된 키를 SHA-256으로 해시하여 항상 32바이트(256비트) 키를 생성합니다.
     * 이렇게 하면 어떤 길이의 키를 입력해도 AES-256에 적합한 키가 됩니다.</p>
     * 
     * @param encryptionKey 환경변수에서 주입받은 암호화 키 (임의 길이)
     */
    public EncryptionUtil(@Value("${app.encryption.key}") String encryptionKey) {
        try {
            // SHA-256 해시를 사용하여 항상 32바이트 키 생성
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(encryptionKey.getBytes(StandardCharsets.UTF_8));
            
            this.secretKey = new SecretKeySpec(keyBytes, "AES");
            log.info("EncryptionUtil initialized with AES-256-GCM (key derived via SHA-256)");
            
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * 문자열 암호화.
     * 
     * <p>평문을 AES-256-GCM으로 암호화하고 Base64로 인코딩합니다.
     * IV는 암호문 앞에 붙여서 저장됩니다.</p>
     * 
     * @param plainText 암호화할 평문
     * @return Base64로 인코딩된 암호문 (IV + 암호문)
     * @throws RuntimeException 암호화 실패 시
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }

        try {
            // 랜덤 IV 생성
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);

            // Cipher 초기화
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            // 암호화 수행
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // IV + 암호문을 합쳐서 저장 (복호화 시 IV 필요)
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + encryptedBytes.length);
            byteBuffer.put(iv);
            byteBuffer.put(encryptedBytes);

            // Base64 인코딩하여 반환
            String encrypted = Base64.getEncoder().encodeToString(byteBuffer.array());
            log.debug("Encryption successful. Original length: {}, Encrypted length: {}", 
                    plainText.length(), encrypted.length());
            
            return encrypted;

        } catch (Exception e) {
            log.error("Encryption failed: {}", e.getMessage());
            throw new RuntimeException("Failed to encrypt data", e);
        }
    }

    /**
     * 암호문 복호화.
     * 
     * <p>Base64로 인코딩된 암호문을 복호화하여 평문을 반환합니다.
     * 암호문 앞의 IV를 추출하여 복호화에 사용합니다.</p>
     * 
     * @param encryptedText Base64로 인코딩된 암호문 (IV + 암호문)
     * @return 복호화된 평문
     * @throws RuntimeException 복호화 실패 시
     */
    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }

        try {
            // Base64 디코딩
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedText);

            // IV와 암호문 분리
            ByteBuffer byteBuffer = ByteBuffer.wrap(encryptedBytes);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);
            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);

            // Cipher 초기화
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            // 복호화 수행
            byte[] decryptedBytes = cipher.doFinal(cipherText);
            String decrypted = new String(decryptedBytes, StandardCharsets.UTF_8);
            
            log.debug("Decryption successful. Encrypted length: {}, Decrypted length: {}", 
                    encryptedText.length(), decrypted.length());
            
            return decrypted;

        } catch (Exception e) {
            log.error("Decryption failed: {}", e.getMessage());
            throw new RuntimeException("Failed to decrypt data", e);
        }
    }

    /**
     * 데이터가 암호화되어 있는지 확인.
     * 
     * <p>Base64 디코딩 가능 여부와 최소 길이로 판단합니다.
     * (IV 12바이트 + 최소 암호문 + 태그)</p>
     * 
     * @param data 확인할 데이터
     * @return 암호화된 데이터로 추정되면 true
     */
    public boolean isEncrypted(String data) {
        if (data == null || data.isEmpty()) {
            return false;
        }
        
        try {
            byte[] decoded = Base64.getDecoder().decode(data);
            // IV(12) + 최소 암호문(1) + 태그(16) = 최소 29바이트
            return decoded.length >= 29;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
