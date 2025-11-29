package vibe.scon.scon_backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 매장 엔티티.
 * 
 * <p>사장님이 운영하는 개별 매장 정보를 저장합니다.
 * 매장에는 여러 직원이 소속되고, 여러 스케줄이 생성됩니다.</p>
 * 
 * <h3>테이블 정보:</h3>
 * <ul>
 *   <li>테이블명: {@code stores}</li>
 *   <li>기본키: {@code id} (AUTO_INCREMENT)</li>
 *   <li>외래키: {@code owner_id} → {@code owners.id}</li>
 * </ul>
 * 
 * <h3>요구사항 추적 (Traceability):</h3>
 * <ul>
 *   <li>{@code REQ-FUNC-002} - 매장 정보 수집 (업종, 영업시간 등)</li>
 *   <li>{@code SRS §6.2.0} - ERD: STORE 엔티티</li>
 *   <li>{@code SRS §6.2.1} - Store 데이터 모델</li>
 * </ul>
 * 
 * @see Owner
 * @see Employee
 * @see Schedule
 * @see <a href="docs/GPT-SRS_v0.2.md">SRS §6.2.1 Store</a>
 * @see <a href="tasks/github-issues/issue-003-REQ-FUNC-001-003.md">Issue-003: 매장 관리 API</a>
 */
@Entity
@Table(name = "stores")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Store extends BaseEntity {

    /**
     * 매장명
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * 업종 (베이커리, 카페 등)
     */
    @Column(name = "business_type", length = 50)
    private String businessType;

    /**
     * 매장 주소
     */
    @Column(length = 255)
    private String address;

    /**
     * 영업 시작 시간
     */
    @Column(name = "open_time")
    private LocalTime openTime;

    /**
     * 영업 종료 시간
     */
    @Column(name = "close_time")
    private LocalTime closeTime;

    /**
     * 매장 소유자 (ManyToOne)
     */
    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Owner owner;

    /**
     * 소속 직원 목록 (양방향 관계)
     */
    @OneToMany(mappedBy = "store", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Employee> employees = new ArrayList<>();

    /**
     * 스케줄 목록 (양방향 관계)
     */
    @OneToMany(mappedBy = "store", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Schedule> schedules = new ArrayList<>();

    /**
     * Store 엔티티 생성자
     * 
     * @param name 매장명
     * @param businessType 업종
     * @param address 주소
     * @param openTime 영업 시작 시간
     * @param closeTime 영업 종료 시간
     * @param owner 소유자
     */
    @Builder
    public Store(String name, String businessType, String address,
                 LocalTime openTime, LocalTime closeTime, Owner owner) {
        this.name = name;
        this.businessType = businessType;
        this.address = address;
        this.openTime = openTime;
        this.closeTime = closeTime;
        this.owner = owner;
    }

    /**
     * 직원 추가 연관관계 편의 메서드
     * 
     * @param employee 추가할 직원
     */
    public void addEmployee(Employee employee) {
        employees.add(employee);
        employee.setStore(this);
    }

    /**
     * 스케줄 추가 연관관계 편의 메서드
     * 
     * @param schedule 추가할 스케줄
     */
    public void addSchedule(Schedule schedule) {
        schedules.add(schedule);
        schedule.setStore(this);
    }

    /**
     * 매장 정보 업데이트.
     * 
     * <p>JPA dirty checking을 통해 변경사항을 자동 반영합니다.</p>
     * 
     * @param name 매장명
     * @param businessType 업종
     * @param address 주소
     * @param openTime 영업 시작 시간
     * @param closeTime 영업 종료 시간
     */
    public void update(String name, String businessType, String address,
                       LocalTime openTime, LocalTime closeTime) {
        if (name != null) this.name = name;
        if (businessType != null) this.businessType = businessType;
        if (address != null) this.address = address;
        if (openTime != null) this.openTime = openTime;
        if (closeTime != null) this.closeTime = closeTime;
    }
}

