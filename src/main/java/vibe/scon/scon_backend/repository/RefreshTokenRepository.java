package vibe.scon.scon_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vibe.scon.scon_backend.entity.RefreshToken;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Refresh Token Repository.
 * 
 * <p>Refresh Token 엔티티에 대한 데이터 접근 계층입니다.
 * 토큰 조회, 삭제, 만료 토큰 정리 등의 기능을 제공합니다.</p>
 * 
 * <h3>요구사항 추적 (Traceability):</h3>
 * <ul>
 *   <li>{@code POC-BE-FUNC-003} - 로그아웃 API 구현</li>
 *   <li>{@code AC-002} - Refresh Token 무효화</li>
 * </ul>
 * 
 * @see RefreshToken
 * @see <a href="../../SCON-Update-Plan/POC-BE-FUNC-003-logout.md">POC-BE-FUNC-003</a>
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * 토큰 해시값으로 Refresh Token 조회.
     * 
     * <p>로그아웃 시 특정 토큰을 찾거나, 토큰 갱신 시 유효성 검증에 사용됩니다.</p>
     * 
     * @param token 토큰 해시값 (SHA-256)
     * @return RefreshToken 엔티티 (없으면 Optional.empty())
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * 사용자 ID로 모든 Refresh Token 조회.
     * 
     * <p>한 사용자가 여러 기기에서 로그인한 경우 여러 토큰이 존재할 수 있습니다.
     * 모든 기기에서 로그아웃하는 기능에 사용됩니다.</p>
     * 
     * @param ownerId 사용자 ID
     * @return 해당 사용자의 모든 Refresh Token 목록
     */
    List<RefreshToken> findByOwnerId(Long ownerId);

    /**
     * 토큰 해시값으로 Refresh Token 삭제.
     * 
     * <p>로그아웃 시 특정 토큰을 무효화하는 데 사용됩니다.</p>
     * 
     * @param token 토큰 해시값 (SHA-256)
     */
    void deleteByToken(String token);

    /**
     * 사용자 ID로 모든 Refresh Token 삭제.
     * 
     * <p>모든 기기에서 동시 로그아웃하는 기능에 사용됩니다.</p>
     * 
     * @param ownerId 사용자 ID
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.ownerId = :ownerId")
    void deleteByOwnerId(@Param("ownerId") Long ownerId);

    /**
     * 만료된 Refresh Token 삭제.
     * 
     * <p>주기적으로 실행되는 스케줄링 작업에서 사용됩니다.
     * 만료 시간이 현재 시간보다 이전인 토큰을 삭제합니다.</p>
     * 
     * @param now 현재 시간
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now")
    void deleteByExpiresAtBefore(@Param("now") LocalDateTime now);
}

