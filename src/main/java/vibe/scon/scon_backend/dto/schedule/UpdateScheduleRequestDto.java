package vibe.scon.scon_backend.dto.schedule;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import vibe.scon.scon_backend.entity.enums.ScheduleStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * 스케줄 수정 요청 DTO.
 * 
 * <p>스케줄 상태 및 Shift 정보를 수정하기 위한 DTO입니다.</p>
 * 
 * <h3>부분 수정 지원:</h3>
 * <ul>
 *   <li>status가 null인 경우: 상태 변경 없음</li>
 *   <li>shifts가 null인 경우: Shift 정보 변경 없음</li>
 *   <li>둘 다 null이 아닌 경우: 둘 다 업데이트</li>
 * </ul>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateScheduleRequestDto {

    /**
     * 스케줄 상태 (선택적).
     * 
     * <p>null인 경우 상태 변경 없음.</p>
     */
    private ScheduleStatus status;

    /**
     * Shift 목록 (선택적).
     * 
     * <p>null인 경우 Shift 정보 변경 없음.</p>
     * <p>빈 배열인 경우: 모든 Shift 삭제</p>
     * <p>값이 있는 경우: 전체 교체 (Full Replace 방식)</p>
     */
    @Valid
    @Size(max = 100, message = "Shift는 최대 100개까지 가능합니다")
    private List<ShiftRequestDto> shifts;

    /**
     * status만 업데이트 여부 확인.
     * 
     * @return true인 경우 status만 업데이트
     */
    @JsonIgnore
    public boolean isStatusOnlyUpdate() {
        return status != null && (shifts == null);
    }

    /**
     * shifts만 업데이트 여부 확인.
     * 
     * @return true인 경우 shifts만 업데이트
     */
    @JsonIgnore
    public boolean isShiftsOnlyUpdate() {
        return shifts != null && status == null;
    }

    /**
     * 둘 다 업데이트 여부 확인.
     * 
     * @return true인 경우 둘 다 업데이트
     */
    @JsonIgnore
    public boolean isFullUpdate() {
        return status != null && shifts != null;
    }

    /**
     * 업데이트할 내용이 있는지 확인.
     * 
     * @return true인 경우 업데이트할 내용이 있음
     */
    @JsonIgnore
    public boolean hasUpdate() {
        return status != null || shifts != null;
    }

    /**
     * 빈 shifts 리스트 반환 (null 방지).
     * 
     * @return 빈 리스트 또는 기존 shifts
     */
    @JsonIgnore
    public List<ShiftRequestDto> getShiftsOrEmpty() {
        return shifts != null ? shifts : new ArrayList<>();
    }
}
