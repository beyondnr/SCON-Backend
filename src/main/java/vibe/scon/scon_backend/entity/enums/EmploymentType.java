package vibe.scon.scon_backend.entity.enums;

/**
 * 직원 고용 형태를 정의하는 열거형.
 * 
 * <p>직원의 근무 유형에 따라 급여 계산 방식과 노동법 적용이 달라질 수 있습니다.</p>
 * 
 * <ul>
 *   <li>{@code MANAGER} - 매니저 (매장 관리 역할)</li>
 *   <li>{@code EMPLOYEE} - 일반 직원 (일반 업무 역할)</li>
 * </ul>
 * 
 * <h3>요구사항 추적 (Traceability):</h3>
 * <ul>
 *   <li>{@code REQ-FUNC-003} - 직원 등록 (고용형태 필드)</li>
 *   <li>{@code SRS §6.2.2} - Employee.employmentType</li>
 * </ul>
 * 
 * @see vibe.scon.scon_backend.entity.Employee
 * @see <a href="docs/GPT-SRS_v0.2.md">SRS §6.2.2 Employee</a>
 */
public enum EmploymentType {
    
    /**
     * 매니저 - 매장 관리 역할을 수행하는 직원
     */
    MANAGER,
    
    /**
     * 일반 직원 - 일반적인 업무를 수행하는 직원
     */
    EMPLOYEE
}

