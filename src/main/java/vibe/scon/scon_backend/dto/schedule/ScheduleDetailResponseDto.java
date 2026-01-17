package vibe.scon.scon_backend.dto.schedule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import vibe.scon.scon_backend.entity.Schedule;
import vibe.scon.scon_backend.entity.enums.ScheduleStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 스케줄 상세 응답 DTO.
 * 
 * <p>스케줄 상세 조회 시 Shift 정보를 포함하는 DTO입니다.</p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleDetailResponseDto {

    private Long id;
    private LocalDate weekStartDate;
    private ScheduleStatus status;
    private Long storeId;
    private List<ShiftResponseDto> shifts;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Schedule 엔티티로부터 DTO 생성.
     * 
     * <p>Shift 정보를 포함하여 변환합니다.</p>
     * 
     * @param schedule Schedule 엔티티 (Shift 정보 포함)
     * @return ScheduleDetailResponseDto
     */
    public static ScheduleDetailResponseDto from(Schedule schedule) {
        List<ShiftResponseDto> shiftDtos = new ArrayList<>();
        
        // Shift 정보가 로드되어 있는 경우 변환
        if (schedule.getShifts() != null && !schedule.getShifts().isEmpty()) {
            shiftDtos = schedule.getShifts().stream()
                    .map(ShiftResponseDto::from)
                    .collect(Collectors.toList());
        }

        return ScheduleDetailResponseDto.builder()
                .id(schedule.getId())
                .weekStartDate(schedule.getWeekStartDate())
                .status(schedule.getStatus())
                .storeId(schedule.getStore().getId())
                .shifts(shiftDtos)
                .createdAt(schedule.getCreatedAt())
                .updatedAt(schedule.getUpdatedAt())
                .build();
    }
}
