package vibe.scon.scon_backend.dto.onboarding;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 온보딩 완료 응답 DTO.
 * 
 * <h3>요구사항 추적 (Traceability):</h3>
 * <ul>
 *   <li>{@code REQ-FUNC-001} - 3단계 온보딩 마법사</li>
 *   <li>{@code AC-005} - 온보딩 완료 및 첫 스케줄 생성</li>
 *   <li>{@code TC-ONBOARD-002} - Draft 스케줄 자동 생성</li>
 * </ul>
 * 
 * @see <a href="tasks/github-issues/issue-003-REQ-FUNC-001-003.md">Issue-003 §9.4</a>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingCompleteResponseDto {

    /**
     * 온보딩 완료 여부.
     */
    private boolean completed;

    /**
     * 매장 ID.
     */
    private Long storeId;

    /**
     * 매장명.
     */
    private String storeName;

    /**
     * 생성된 첫 스케줄 ID.
     */
    private Long scheduleId;

    /**
     * 생성된 스케줄의 주 시작일.
     */
    private LocalDate weekStartDate;

    /**
     * 생성된 스케줄의 상태 (DRAFT).
     */
    private String scheduleStatus;

    /**
     * 안내 메시지.
     */
    private String message;
}
