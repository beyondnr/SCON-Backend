package vibe.scon.scon_backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vibe.scon.scon_backend.entity.enums.EmploymentType;
import vibe.scon.scon_backend.entity.enums.ShiftPreset;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 직원 엔티티.
 * 
 * <p>매장에 소속된 정규직/비정규직 직원 정보를 저장합니다.
 * 직원은 여러 시프트에 배정되고, 가용시간을 제출할 수 있습니다.</p>
 * 
 * <h3>테이블 정보:</h3>
 * <ul>
 *   <li>테이블명: {@code employees}</li>
 *   <li>기본키: {@code id} (AUTO_INCREMENT)</li>
 *   <li>외래키: {@code store_id} → {@code stores.id}</li>
 * </ul>
 * 
 * <h3>요구사항 추적 (Traceability):</h3>
 * <ul>
 *   <li>{@code REQ-FUNC-003} - 직원 등록 (이름, 고용형태, 시급, 연락처)</li>
 *   <li>{@code REQ-NF-007} - 저장 데이터 암호화: phone 필드 AES-256 암호화 필요</li>
 *   <li>{@code REQ-NF-010} - PII 최소 수집: 필드 8개 이하 제한</li>
 *   <li>{@code SRS §6.2.0} - ERD: EMPLOYEE 엔티티</li>
 *   <li>{@code SRS §6.2.2} - Employee 데이터 모델</li>
 * </ul>
 * 
 * @see Store
 * @see Shift
 * @see AvailabilitySubmission
 * @see EmploymentType
 * @see <a href="docs/GPT-SRS_v0.2.md">SRS §6.2.2 Employee</a>
 * @see <a href="tasks/github-issues/issue-003-REQ-FUNC-001-003.md">Issue-003: 직원 관리 API</a>
 */
@Entity
@Table(name = "employees")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Employee extends BaseEntity {

    /**
     * 직원 이름
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * 연락처 (선택).
     * 
     * <p><b>보안 요구사항 (REQ-NF-007):</b> 
     * DB 저장 시 AES-256-GCM 암호화 필수. 
     * API 응답 시 복호화하여 반환.</p>
     * 
     * @see <a href="tasks/github-issues/issue-003-REQ-FUNC-001-003.md">TC-EMP-002: PII 암호화 저장 검증</a>
     */
    @Column(length = 255)  // 암호화 시 길이 증가
    private String phone;

    /**
     * 직원 이메일 (알림 발송용)
     */
    @Column(length = 255)
    private String email;

    /**
     * 시급 (DECIMAL(10,2))
     */
    @Column(name = "hourly_wage", precision = 10, scale = 2)
    private BigDecimal hourlyWage;

    /**
     * 고용 형태 (정규직/비정규직)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", nullable = false, length = 20)
    private EmploymentType employmentType;

    /**
     * 근무 시프트 프리셋 (MORNING, AFTERNOON, CUSTOM)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "shift_preset")
    private ShiftPreset shiftPreset;

    /**
     * 사용자 정의 근무 시작 시간
     */
    @Column(name = "custom_shift_start_time")
    private LocalTime customShiftStartTime;

    /**
     * 사용자 정의 근무 종료 시간
     */
    @Column(name = "custom_shift_end_time")
    private LocalTime customShiftEndTime;

    /**
     * 개인 휴무일
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "personal_holiday")
    private DayOfWeek personalHoliday;

    /**
     * 소속 매장 (ManyToOne)
     */
    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    /**
     * 배정된 시프트 목록 (양방향 관계)
     */
    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Shift> shifts = new ArrayList<>();

    /**
     * 제출한 가용시간 목록 (양방향 관계)
     */
    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AvailabilitySubmission> availabilities = new ArrayList<>();

    /**
     * Employee 엔티티 생성자
     * 
     * @param name 직원 이름
     * @param phone 연락처
     * @param email 이메일
     * @param hourlyWage 시급
     * @param employmentType 고용 형태
     * @param store 소속 매장
     */
    @Builder
    public Employee(String name, String phone, String email, BigDecimal hourlyWage,
                    EmploymentType employmentType, Store store,
                    ShiftPreset shiftPreset, LocalTime customShiftStartTime, LocalTime customShiftEndTime,
                    DayOfWeek personalHoliday) {
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.hourlyWage = hourlyWage;
        this.employmentType = employmentType;
        this.store = store;
        this.shiftPreset = shiftPreset;
        this.customShiftStartTime = customShiftStartTime;
        this.customShiftEndTime = customShiftEndTime;
        this.personalHoliday = personalHoliday;
    }

    /**
     * 직원 정보 업데이트.
     * 
     * <p>JPA dirty checking을 통해 변경사항을 자동 반영합니다.</p>
     * 
     * @param name 직원 이름
     * @param phone 연락처 (암호화된 값)
     * @param email 이메일
     * @param hourlyWage 시급
     * @param employmentType 고용 형태
     * @param shiftPreset 근무 프리셋
     * @param customShiftStartTime 사용자 정의 시작 시간
     * @param customShiftEndTime 사용자 정의 종료 시간
     */
    public void update(String name, String phone, String email, BigDecimal hourlyWage, EmploymentType employmentType,
                       ShiftPreset shiftPreset, LocalTime customShiftStartTime, LocalTime customShiftEndTime,
                       DayOfWeek personalHoliday) {
        if (name != null) this.name = name;
        if (phone != null) this.phone = phone;
        if (email != null) this.email = email;
        if (hourlyWage != null) this.hourlyWage = hourlyWage;
        if (employmentType != null) this.employmentType = employmentType;
        if (shiftPreset != null) this.shiftPreset = shiftPreset;
        this.customShiftStartTime = customShiftStartTime; // Nullable update allow
        this.customShiftEndTime = customShiftEndTime;     // Nullable update allow
        this.personalHoliday = personalHoliday;           // Nullable update allow
    }
}

