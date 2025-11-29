package vibe.scon.scon_backend.dto.employee;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import vibe.scon.scon_backend.entity.Employee;
import vibe.scon.scon_backend.entity.enums.EmploymentType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 직원 응답 DTO.
 * 
 * <h3>요구사항 추적 (Traceability):</h3>
 * <ul>
 *   <li>{@code REQ-FUNC-003} - 직원 등록</li>
 *   <li>{@code AC-004} - 직원 등록 및 PII 암호화</li>
 *   <li>{@code TC-EMP-003} - PII 복호화 응답 검증</li>
 *   <li>{@code TC-EMP-006} - 직원 목록 조회 API</li>
 * </ul>
 * 
 * @see <a href="tasks/github-issues/issue-003-REQ-FUNC-001-003.md">Issue-003 §9.3</a>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeResponseDto {

    /**
     * 직원 ID.
     */
    private Long id;

    /**
     * 직원 이름.
     */
    private String name;

    /**
     * 연락처 (복호화된 값).
     * 
     * <p>API 응답 시 복호화하여 반환 (TC-EMP-003).</p>
     */
    private String phone;

    /**
     * 시급.
     */
    private BigDecimal hourlyWage;

    /**
     * 고용 형태.
     */
    private EmploymentType employmentType;

    /**
     * 소속 매장 ID.
     */
    private Long storeId;

    /**
     * 소속 매장명.
     */
    private String storeName;

    /**
     * 생성 시각.
     */
    private LocalDateTime createdAt;

    /**
     * 수정 시각.
     */
    private LocalDateTime updatedAt;

    /**
     * Entity → DTO 변환 (복호화된 phone 포함).
     * 
     * <p>phone 필드는 이미 복호화된 값을 전달받아야 합니다.</p>
     * 
     * @param employee Employee 엔티티
     * @param decryptedPhone 복호화된 연락처
     * @return EmployeeResponseDto
     */
    public static EmployeeResponseDto from(Employee employee, String decryptedPhone) {
        return EmployeeResponseDto.builder()
                .id(employee.getId())
                .name(employee.getName())
                .phone(decryptedPhone)
                .hourlyWage(employee.getHourlyWage())
                .employmentType(employee.getEmploymentType())
                .storeId(employee.getStore().getId())
                .storeName(employee.getStore().getName())
                .createdAt(employee.getCreatedAt())
                .updatedAt(employee.getUpdatedAt())
                .build();
    }

    /**
     * Entity → DTO 변환 (암호화된 phone 그대로 사용).
     * 
     * <p>내부용 또는 phone이 null인 경우 사용.</p>
     * 
     * @param employee Employee 엔티티
     * @return EmployeeResponseDto
     */
    public static EmployeeResponseDto from(Employee employee) {
        return EmployeeResponseDto.builder()
                .id(employee.getId())
                .name(employee.getName())
                .phone(employee.getPhone())  // 암호화된 값 또는 null
                .hourlyWage(employee.getHourlyWage())
                .employmentType(employee.getEmploymentType())
                .storeId(employee.getStore().getId())
                .storeName(employee.getStore().getName())
                .createdAt(employee.getCreatedAt())
                .updatedAt(employee.getUpdatedAt())
                .build();
    }
}
