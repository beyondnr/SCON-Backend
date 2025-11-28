package vibe.scon.scon_backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vibe.scon.scon_backend.entity.enums.EmploymentType;

import java.math.BigDecimal;
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
 * @see Store
 * @see Shift
 * @see AvailabilitySubmission
 * @see EmploymentType
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
     * 연락처 (선택)
     */
    @Column(length = 20)
    private String phone;

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
     * @param hourlyWage 시급
     * @param employmentType 고용 형태
     * @param store 소속 매장
     */
    @Builder
    public Employee(String name, String phone, BigDecimal hourlyWage,
                    EmploymentType employmentType, Store store) {
        this.name = name;
        this.phone = phone;
        this.hourlyWage = hourlyWage;
        this.employmentType = employmentType;
        this.store = store;
    }
}

