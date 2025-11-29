package vibe.scon.scon_backend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 로그인 요청 DTO.
 * 
 * <h3>요구사항 추적 (Traceability):</h3>
 * <ul>
 *   <li>{@code REQ-FUNC-001} - 3단계 온보딩 마법사 (로그인)</li>
 *   <li>{@code AC-002} - 사장님 로그인</li>
 *   <li>{@code TC-AUTH-004} - 로그인 성공 + JWT 발급</li>
 *   <li>{@code TC-AUTH-005} - 잘못된 비밀번호 로그인 실패</li>
 * </ul>
 * 
 * @see <a href="tasks/github-issues/issue-003-REQ-FUNC-001-003.md">Issue-003 §9.1</a>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestDto {

    /**
     * 이메일 주소 (로그인 ID).
     */
    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String email;

    /**
     * 비밀번호.
     */
    @NotBlank(message = "비밀번호는 필수입니다")
    private String password;
}
