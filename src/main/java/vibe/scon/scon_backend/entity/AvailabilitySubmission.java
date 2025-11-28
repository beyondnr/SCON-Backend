package vibe.scon.scon_backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 직원 가용시간 제출 엔티티.
 * 
 * <p>직원이 특정 주에 근무 가능한 시간대를 제출합니다.
 * 요일별로 가용 시간대를 저장하며, 스케줄 생성 시 참고됩니다.</p>
 * 
 * <h3>테이블 정보:</h3>
 * <ul>
 *   <li>테이블명: {@code availability_submissions}</li>
 *   <li>기본키: {@code id} (AUTO_INCREMENT)</li>
 *   <li>외래키: {@code employee_id} → {@code employees.id}</li>
 * </ul>
 * 
 * <p>Note: {@code dayOfWeek} 필드는 Java 표준 {@link java.time.DayOfWeek}를 사용합니다.</p>
 * 
 * @see Employee
 * @see java.time.DayOfWeek
 */
@Entity
@Table(name = "availability_submissions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AvailabilitySubmission extends BaseEntity {

    /**
     * 주 시작일 (월요일 기준)
     */
    @Column(name = "week_start_date", nullable = false)
    private LocalDate weekStartDate;

    /**
     * 요일 (MONDAY ~ SUNDAY)
     * Java 표준 java.time.DayOfWeek 사용
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 10)
    private DayOfWeek dayOfWeek;

    /**
     * 가용 시작 시간
     */
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    /**
     * 가용 종료 시간
     */
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    /**
     * 제출 직원 (ManyToOne)
     */
    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    /**
     * AvailabilitySubmission 엔티티 생성자
     * 
     * @param weekStartDate 주 시작일
     * @param dayOfWeek 요일
     * @param startTime 가용 시작 시간
     * @param endTime 가용 종료 시간
     * @param employee 직원
     */
    @Builder
    public AvailabilitySubmission(LocalDate weekStartDate, DayOfWeek dayOfWeek,
                                   LocalTime startTime, LocalTime endTime,
                                   Employee employee) {
        this.weekStartDate = weekStartDate;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.employee = employee;
    }
}

