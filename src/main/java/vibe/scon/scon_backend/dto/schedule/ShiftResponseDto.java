package vibe.scon.scon_backend.dto.schedule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import vibe.scon.scon_backend.entity.Shift;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Shift 응답 DTO.
 * 
 * <p>스케줄 상세 조회 시 Shift 정보를 포함하기 위한 DTO입니다.</p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShiftResponseDto {

    private Long id;
    private Long employeeId;
    private LocalDate workDate;
    private LocalTime startTime;
    private LocalTime endTime;

    /**
     * Shift 엔티티로부터 DTO 생성.
     * 
     * @param shift Shift 엔티티
     * @return ShiftResponseDto
     */
    public static ShiftResponseDto from(Shift shift) {
        return ShiftResponseDto.builder()
                .id(shift.getId())
                .employeeId(shift.getEmployee().getId())
                .workDate(shift.getWorkDate())
                .startTime(shift.getStartTime())
                .endTime(shift.getEndTime())
                .build();
    }
}
