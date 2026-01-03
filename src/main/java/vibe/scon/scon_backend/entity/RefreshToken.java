package vibe.scon.scon_backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Refresh Token 엔티티.
 * 
 * <p>JWT Refresh Token을 DB에 저장하여 로그아웃 시 무효화할 수 있도록 합니다.
 * 토큰은 해시값(SHA-256)으로 저장하여 보안을 강화합니다.</p>
 * 
 * <h3>테이블 정보:</h3>
 * <ul>
 *   <li>테이블명: {@code refresh_tokens}</li>
 *   <li>기본키: {@code id} (AUTO_INCREMENT)</li>
 *   <li>유니크 제약: {@code token} (해시값)</li>
 *   <li>인덱스: {@code owner_id}, {@code expires_at}</li>
 * </ul>
 * 
 * <h3>요구사항 추적 (Traceability):</h3>
 * <ul>
 *   <li>{@code POC-BE-FUNC-003} - 로그아웃 API 구현</li>
 *   <li>{@code AC-002} - Refresh Token 무효화</li>
 * </ul>
 * 
 * <h3>설계 고려사항:</h3>
 * <ul>
 *   <li>{@code token} 필드는 해시값 저장 (보안 강화)</li>
 *   <li>{@code expiresAt} 필드로 만료 시간 관리</li>
 *   <li>{@code ownerId}로 사용자별 토큰 관리</li>
 * </ul>
 * 
 * @see <a href="../../SCON-Update-Plan/POC-BE-FUNC-003-logout.md">POC-BE-FUNC-003</a>
 */
@Entity
@Table(name = "refresh_tokens", indexes = {
    @Index(name = "idx_refresh_tokens_owner_id", columnList = "owner_id"),
    @Index(name = "idx_refresh_tokens_expires_at", columnList = "expires_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends BaseEntity {

    /**
     * Refresh Token의 해시값 (SHA-256).
     * 
     * <p>보안을 위해 평문 토큰이 아닌 해시값을 저장합니다.
     * 유니크 제약으로 중복 저장을 방지합니다.</p>
     */
    @Column(nullable = false, unique = true, length = 255)
    private String token;

    /**
     * 토큰 소유자 ID (Owner의 ID).
     * 
     * <p>사용자별 토큰 관리를 위해 Owner ID를 저장합니다.
     * 외래키 제약은 없지만, Owner 엔티티와의 관계를 나타냅니다.</p>
     */
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    /**
     * 토큰 만료 시간.
     * 
     * <p>Refresh Token의 만료 시간을 저장합니다.
     * 만료된 토큰은 주기적으로 정리됩니다.</p>
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * 토큰 사용 여부.
     * 
     * <p>Refresh Token이 이미 사용되었는지 추적합니다.
     * 재사용 탐지를 위해 사용됩니다.</p>
     * 
     * <h3>요구사항 추적 (Traceability):</h3>
     * <ul>
     *   <li>{@code POC-BE-SEC-002} - 백엔드 보안 강화 (JWT 토큰 Rotation)</li>
     * </ul>
     * 
     * @see <a href="../../SCON-Update-Plan/POC-BE-SEC-002.md">POC-BE-SEC-002</a>
     */
    @Column(name = "is_used", nullable = false)
    private boolean isUsed = false;

    /**
     * RefreshToken 엔티티 생성자.
     * 
     * @param token Refresh Token의 해시값 (SHA-256)
     * @param ownerId 토큰 소유자 ID
     * @param expiresAt 토큰 만료 시간
     */
    @Builder
    public RefreshToken(String token, Long ownerId, LocalDateTime expiresAt) {
        this.token = token;
        this.ownerId = ownerId;
        this.expiresAt = expiresAt;
        this.isUsed = false;
    }

    /**
     * 토큰을 사용된 것으로 표시.
     * 
     * <p>Refresh Token이 사용되었음을 표시합니다.
     * 재사용 탐지를 위해 호출됩니다.</p>
     */
    public void markAsUsed() {
        this.isUsed = true;
    }

    /**
     * 토큰이 만료되었는지 확인.
     * 
     * @return 만료되었으면 true, 그렇지 않으면 false
     */
    public boolean isExpired() {
        return expiresAt.isBefore(LocalDateTime.now());
    }
}

