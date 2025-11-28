package vibe.scon.scon_backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vibe.scon.scon_backend.entity.enums.ScheduleStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 스케줄 엔티티.
 * 
 * <p>매장의 주간 스케줄을 관리합니다. 스케줄은 특정 주(week_start_date)에 대한
 * 근무 시프트 목록을 포함하며, 상태(DRAFT → PUBLISHED)에 따라 워크플로우가 진행됩니다.</p>
 * 
 * <h3>테이블 정보:</h3>
 * <ul>
 *   <li>테이블명: {@code schedules}</li>
 *   <li>기본키: {@code id} (AUTO_INCREMENT)</li>
 *   <li>외래키: {@code store_id} → {@code stores.id}</li>
 *   <li>복합 유니크: {@code (store_id, week_start_date)}</li>
 * </ul>
 * 
 * @see Store
 * @see Shift
 * @see ScheduleStatus
 */
@Entity
@Table(
    name = "schedules",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_schedule_store_week",
            columnNames = {"store_id", "week_start_date"}
        )
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Schedule extends BaseEntity {

    /**
     * 주 시작일 (월요일 기준)
     */
    @Column(name = "week_start_date", nullable = false)
    private LocalDate weekStartDate;

    /**
     * 스케줄 상태 (DRAFT, PENDING, APPROVED, PUBLISHED)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScheduleStatus status;

    /**
     * 매장 (ManyToOne)
     */
    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    /**
     * 시프트 목록 (양방향 관계)
     */
    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Shift> shifts = new ArrayList<>();

    /**
     * Schedule 엔티티 생성자
     * 
     * @param weekStartDate 주 시작일
     * @param status 스케줄 상태
     * @param store 매장
     */
    @Builder
    public Schedule(LocalDate weekStartDate, ScheduleStatus status, Store store) {
        this.weekStartDate = weekStartDate;
        this.status = status != null ? status : ScheduleStatus.DRAFT;
        this.store = store;
    }

    /**
     * 시프트 추가 연관관계 편의 메서드
     * 
     * @param shift 추가할 시프트
     */
    public void addShift(Shift shift) {
        shifts.add(shift);
        shift.setSchedule(this);
    }

    /**
     * 스케줄 상태 변경
     * 
     * @param newStatus 새로운 상태
     */
    public void changeStatus(ScheduleStatus newStatus) {
        this.status = newStatus;
    }
}

