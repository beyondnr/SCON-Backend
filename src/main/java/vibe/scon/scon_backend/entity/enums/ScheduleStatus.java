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
 * 
 * <h3>요구사항 추적 (Traceability):</h3>
 * <ul>
 *   <li>{@code REQ-FUNC-006} - 승인 대기 대시보드 (PENDING 상태)</li>
 *   <li>{@code REQ-FUNC-008} - 1클릭 스케줄 승인 (DRAFT → APPROVED)</li>
 *   <li>{@code REQ-FUNC-016} - 직원 스케줄 알림 (PUBLISHED 상태)</li>
 *   <li>{@code SRS §6.2.4} - Schedule.status</li>
 * </ul>
 * 
 * @see vibe.scon.scon_backend.entity.Schedule
 * @see <a href="docs/GPT-SRS_v0.2.md">SRS §6.2.4 Schedule</a>
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

