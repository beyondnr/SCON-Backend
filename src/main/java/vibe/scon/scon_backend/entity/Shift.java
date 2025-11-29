package vibe.scon.scon_backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 시프트(근무) 엔티티.
 * 
 * <p>특정 직원이 특정 날짜에 근무하는 시간대를 정의합니다.
 * 시프트는 스케줄에 속하며, 직원에게 배정됩니다.</p>
 * 
 * <h3>테이블 정보:</h3>
 * <ul>
 *   <li>테이블명: {@code shifts}</li>
 *   <li>기본키: {@code id} (AUTO_INCREMENT)</li>
 *   <li>외래키: {@code schedule_id} → {@code schedules.id}</li>
 *   <li>외래키: {@code employee_id} → {@code employees.id}</li>
 * </ul>
 * 
 * <h3>요구사항 추적 (Traceability):</h3>
 * <ul>
 *   <li>{@code REQ-FUNC-007} - 드래그&드롭 스케줄 편집 (시프트 이동)</li>
 *   <li>{@code REQ-FUNC-011} - 자동 급여·수당 계산 (근무시간 기반)</li>
 *   <li>{@code SRS §6.2.0} - ERD: SHIFT 엔티티</li>
 * </ul>
 * 
 * @see Schedule
 * @see Employee
 * @see <a href="docs/GPT-SRS_v0.2.md">SRS §6.2.0 ERD</a>
 */
@Entity
@Table(name = "shifts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Shift extends BaseEntity {

    /**
     * 근무일
     */
    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    /**
     * 근무 시작 시간
     */
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    /**
     * 근무 종료 시간
     */
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    /**
     * 소속 스케줄 (ManyToOne)
     */
    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    /**
     * 배정된 직원 (ManyToOne)
     */
    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    /**
     * Shift 엔티티 생성자
     * 
     * @param workDate 근무일
     * @param startTime 시작 시간
     * @param endTime 종료 시간
     * @param schedule 소속 스케줄
     * @param employee 배정 직원
     */
    @Builder
    public Shift(LocalDate workDate, LocalTime startTime, LocalTime endTime,
                 Schedule schedule, Employee employee) {
        this.workDate = workDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.schedule = schedule;
        this.employee = employee;
    }
}

