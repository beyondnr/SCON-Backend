package vibe.scon.scon_backend.entity.enums;

/**
 * 비동기 작업 상태 열거형.
 * 
 * <p>AsyncTask 엔티티의 상태를 나타냅니다.</p>
 * 
 * @see vibe.scon.scon_backend.entity.AsyncTask
 */
public enum TaskStatus {
    /**
     * 진행 중.
     */
    IN_PROGRESS,
    
    /**
     * 완료됨.
     */
    COMPLETED,
    
    /**
     * 실패함.
     */
    FAILED,
    
    /**
     * 취소됨.
     */
    CANCELLED
}
