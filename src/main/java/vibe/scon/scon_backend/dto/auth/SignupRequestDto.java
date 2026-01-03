package vibe.scon.scon_backend.dto.auth;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원가입 요청 DTO.
 * 
 * <h3>요구사항 추적 (Traceability):</h3>
 * <ul>
 *   <li>{@code REQ-FUNC-001} - 3단계 온보딩 마법사 (회원가입)</li>
 *   <li>{@code AC-001} - 사장님 회원가입 및 JWT 발급</li>
 *   <li>{@code TC-AUTH-001} - 회원가입 성공</li>
 *   <li>{@code TC-AUTH-003} - 중복 이메일 가입 실패</li>
 * </ul>
 * 
 * @see <a href="tasks/github-issues/issue-003-REQ-FUNC-001-003.md">Issue-003 §9.1</a>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignupRequestDto {

    /**
     * 이메일 주소 (로그인 ID로 사용).
     * 
     * <p>형식 검증 및 중복 체크 대상</p>
     */
    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String email;

    /**
     * 비밀번호.
     * 
     * <p>8~20자, 영문/숫자/특수문자 포함. BCrypt로 해시되어 저장됨</p>
     */
    @NotBlank(message = "비밀번호는 필수입니다")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,20}$", 
             message = "비밀번호는 8~20자의 영문, 숫자, 특수문자를 포함해야 합니다")
    private String password;

    /**
     * 사장님 이름.
     */
    @NotBlank(message = "이름은 필수입니다")
    @Size(max = 100, message = "이름은 100자 이내여야 합니다")
    private String name;

    /**
     * 연락처 (선택).
     */
    @Size(max = 20, message = "연락처는 20자 이내여야 합니다")
    private String phone;

    /**
     * 약관 동의 여부.
     */
    @AssertTrue(message = "약관 동의는 필수입니다")
    private Boolean isAgreedToTerms;
}
