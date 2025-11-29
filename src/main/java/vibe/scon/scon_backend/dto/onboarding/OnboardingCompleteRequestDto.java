package vibe.scon.scon_backend.dto.onboarding;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 온보딩 완료 요청 DTO.
 * 
 * <h3>요구사항 추적 (Traceability):</h3>
 * <ul>
 *   <li>{@code REQ-FUNC-001} - 3단계 온보딩 마법사</li>
 *   <li>{@code AC-005} - 온보딩 완료 및 첫 스케줄 생성</li>
 *   <li>{@code TC-ONBOARD-001} - 온보딩 플로우 전체</li>
 *   <li>{@code TC-ONBOARD-002} - Draft 스케줄 자동 생성</li>
 * </ul>
 * 
 * @see <a href="tasks/github-issues/issue-003-REQ-FUNC-001-003.md">Issue-003 §9.4</a>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingCompleteRequestDto {

    /**
     * 온보딩을 완료할 매장 ID.
     */
    @NotNull(message = "매장 ID는 필수입니다")
    private Long storeId;
}
