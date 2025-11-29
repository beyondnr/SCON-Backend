package vibe.scon.scon_backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 토큰 갱신 요청 DTO.
 * 
 * <h3>요구사항 추적 (Traceability):</h3>
 * <ul>
 *   <li>{@code REQ-FUNC-001} - 3단계 온보딩 마법사 (토큰 갱신)</li>
 *   <li>{@code TC-AUTH-006} - 토큰 갱신 API 성공</li>
 *   <li>{@code TC-AUTH-009} - 만료된 Refresh Token 처리</li>
 * </ul>
 * 
 * @see <a href="tasks/github-issues/issue-003-REQ-FUNC-001-003.md">Issue-003 §9.1</a>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenRequestDto {

    /**
     * Refresh Token.
     * 
     * <p>유효한 Refresh Token으로 새 Access Token을 발급받습니다.</p>
     */
    @NotBlank(message = "Refresh Token은 필수입니다")
    private String refreshToken;
}
