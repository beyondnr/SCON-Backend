package vibe.scon.scon_backend.dto.store;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * 매장 생성/수정 요청 DTO.
 * 
 * <h3>요구사항 추적 (Traceability):</h3>
 * <ul>
 *   <li>{@code REQ-FUNC-002} - 매장 정보 수집</li>
 *   <li>{@code AC-003} - 매장 정보 저장 및 조회</li>
 *   <li>{@code TC-STORE-001} - 매장 생성 API</li>
 *   <li>{@code TC-STORE-004} - 매장 수정 API</li>
 * </ul>
 * 
 * @see <a href="tasks/github-issues/issue-003-REQ-FUNC-001-003.md">Issue-003 §9.2</a>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreRequestDto {

    /**
     * 매장명.
     */
    @NotBlank(message = "매장명은 필수입니다")
    @Size(max = 100, message = "매장명은 100자 이내여야 합니다")
    private String name;

    /**
     * 업종 (베이커리, 카페 등).
     */
    @Size(max = 50, message = "업종은 50자 이내여야 합니다")
    private String businessType;

    /**
     * 매장 주소.
     */
    @Size(max = 255, message = "주소는 255자 이내여야 합니다")
    private String address;

    /**
     * 영업 시작 시간.
     */
    private LocalTime openTime;

    /**
     * 영업 종료 시간.
     */
    private LocalTime closeTime;

    /**
     * 정기 휴무일 (선택).
     */
    private DayOfWeek storeHoliday;
}
