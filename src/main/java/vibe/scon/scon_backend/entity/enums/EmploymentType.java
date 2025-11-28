package vibe.scon.scon_backend.entity.enums;

/**
 * 직원 고용 형태를 정의하는 열거형.
 * 
 * <p>직원의 근무 유형에 따라 급여 계산 방식과 노동법 적용이 달라질 수 있습니다.</p>
 * 
 * <ul>
 *   <li>{@code FULL_TIME} - 정규직 (주 40시간 이상 근무)</li>
 *   <li>{@code PART_TIME} - 비정규직/아르바이트 (시간제 근무)</li>
 * </ul>
 */
public enum EmploymentType {
    
    /**
     * 정규직 - 주 40시간 이상 근무하는 직원
     */
    FULL_TIME,
    
    /**
     * 비정규직/아르바이트 - 시간제로 근무하는 직원
     */
    PART_TIME
}

