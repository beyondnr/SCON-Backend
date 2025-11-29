package vibe.scon.scon_backend.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 토큰 응답 DTO.
 * 
 * <p>로그인 및 토큰 갱신 시 반환되는 JWT 토큰 정보입니다.</p>
 * 
 * <h3>요구사항 추적 (Traceability):</h3>
 * <ul>
 *   <li>{@code REQ-FUNC-001} - 3단계 온보딩 마법사 (JWT 발급)</li>
 *   <li>{@code AC-001} - JWT Access Token과 Refresh Token 발급</li>
 *   <li>{@code AC-002} - JWT Access Token (30분 만료) 발급</li>
 *   <li>{@code Issue-003 §7.3} - JWT 스펙</li>
 * </ul>
 * 
 * @see <a href="tasks/github-issues/issue-003-REQ-FUNC-001-003.md">Issue-003 §7.3</a>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponseDto {

    /**
     * Owner ID.
     */
    private Long ownerId;

    /**
     * Owner 이메일.
     */
    private String email;

    /**
     * Access Token.
     * 
     * <p>API 호출 시 Authorization 헤더에 사용.
     * 만료 시간: 30분</p>
     */
    private String accessToken;

    /**
     * Refresh Token.
     * 
     * <p>Access Token 갱신에 사용.
     * 만료 시간: 7일</p>
     */
    private String refreshToken;

    /**
     * 토큰 타입.
     * 
     * <p>항상 "Bearer"</p>
     */
    @Builder.Default
    private String tokenType = "Bearer";

    /**
     * Access Token 만료 시간 (초).
     */
    private Long expiresIn;
}
