package vibe.scon.scon_backend.dto.schedule;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Shift 생성/수정 요청 DTO.
 * 
 * <p>스케줄 수정 시 Shift 정보를 포함하기 위한 DTO입니다.</p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShiftRequestDto {

    /**
     * 직원 ID.
     */
    @NotNull(message = "직원 ID는 필수입니다")
    private Long employeeId;

    /**
     * 근무일.
     */
    @NotNull(message = "근무일은 필수입니다")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate workDate;

    /**
     * 근무 시작 시간.
     */
    @NotNull(message = "시작 시간은 필수입니다")
    @DateTimeFormat(pattern = "HH:mm:ss")
    private LocalTime startTime;

    /**
     * 근무 종료 시간.
     */
    @NotNull(message = "종료 시간은 필수입니다")
    @DateTimeFormat(pattern = "HH:mm:ss")
    private LocalTime endTime;
}
