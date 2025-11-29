package vibe.scon.scon_backend.dto.employee;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import vibe.scon.scon_backend.entity.enums.EmploymentType;

import java.math.BigDecimal;

/**
 * 직원 생성/수정 요청 DTO.
 * 
 * <h3>요구사항 추적 (Traceability):</h3>
 * <ul>
 *   <li>{@code REQ-FUNC-003} - 직원 등록</li>
 *   <li>{@code AC-004} - 직원 등록 및 PII 암호화</li>
 *   <li>{@code TC-EMP-001} - 직원 등록 API</li>
 *   <li>{@code TC-EMP-004} - 직원 수정 API</li>
 * </ul>
 * 
 * @see <a href="tasks/github-issues/issue-003-REQ-FUNC-001-003.md">Issue-003 §9.3</a>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeRequestDto {

    /**
     * 직원 이름.
     */
    @NotBlank(message = "이름은 필수입니다")
    @Size(max = 100, message = "이름은 100자 이내여야 합니다")
    private String name;

    /**
     * 연락처 (PII).
     * 
     * <p>DB 저장 시 AES-256-GCM으로 암호화됩니다 (REQ-NF-007).</p>
     */
    @Size(max = 20, message = "연락처는 20자 이내여야 합니다")
    private String phone;

    /**
     * 시급.
     */
    @PositiveOrZero(message = "시급은 0 이상이어야 합니다")
    private BigDecimal hourlyWage;

    /**
     * 고용 형태 (FULL_TIME / PART_TIME).
     */
    @NotNull(message = "고용 형태는 필수입니다")
    private EmploymentType employmentType;
}
