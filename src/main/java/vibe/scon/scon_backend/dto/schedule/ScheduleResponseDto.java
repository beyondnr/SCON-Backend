package vibe.scon.scon_backend.dto.schedule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import vibe.scon.scon_backend.entity.Schedule;
import vibe.scon.scon_backend.entity.enums.ScheduleStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 스케줄 응답 DTO.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleResponseDto {

    private Long id;
    private LocalDate weekStartDate;
    private ScheduleStatus status;
    private Long storeId;
    
    // 시프트 목록은 필요 시 별도 DTO로 변환하여 포함하거나,
    // 월간 달력 뷰 등 가벼운 조회에서는 제외할 수 있음.
    // 현재 요구사항(월간 조회)에서는 status와 weekStartDate가 핵심이므로
    // 시프트 상세 데이터 포함 여부는 선택적입니다. 일단 빈 리스트로 초기화.
    @Builder.Default
    private List<Object> shifts = new ArrayList<>(); 

    public static ScheduleResponseDto from(Schedule schedule) {
        return ScheduleResponseDto.builder()
                .id(schedule.getId())
                .weekStartDate(schedule.getWeekStartDate())
                .status(schedule.getStatus())
                .storeId(schedule.getStore().getId())
                .build();
    }
}

