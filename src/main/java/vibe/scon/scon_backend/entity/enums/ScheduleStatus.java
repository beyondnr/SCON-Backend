package vibe.scon.scon_backend.entity.enums;

/**
 * 스케줄 상태를 정의하는 열거형.
 * 
 * <p>스케줄은 생성부터 공개까지 4단계 상태를 거칩니다:</p>
 * 
 * <pre>
 * DRAFT → PENDING → APPROVED → PUBLISHED
 * </pre>
 * 
 * <ul>
 *   <li>{@code DRAFT} - 초안 작성 중 (수정 가능)</li>
 *   <li>{@code PENDING} - 검토 대기 (사장님 확인 필요)</li>
 *   <li>{@code APPROVED} - 승인됨 (노동법 검증 완료)</li>
 *   <li>{@code PUBLISHED} - 공개됨 (직원에게 전달 완료)</li>
 * </ul>
 */
public enum ScheduleStatus {
    
    /**
     * 초안 - 작성 중인 상태, 자유롭게 수정 가능
     */
    DRAFT,
    
    /**
     * 검토 대기 - 사장님의 승인을 기다리는 상태
     */
    PENDING,
    
    /**
     * 승인됨 - 노동법 검증을 통과하고 승인된 상태
     */
    APPROVED,
    
    /**
     * 공개됨 - 직원에게 알림이 발송된 최종 상태
     */
    PUBLISHED
}

