package vibe.scon.scon_backend.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 근무 시프트 프리셋.
 * 
 * <p>직원의 선호/고정 근무 시간대를 정의합니다.</p>
 * <ul>
 *   <li>MORNING: 오전조</li>
 *   <li>AFTERNOON: 오후조</li>
 *   <li>CUSTOM: 사용자 정의 시간</li>
 * </ul>
 */
@Getter
@RequiredArgsConstructor
public enum ShiftPreset {
    
    MORNING("오전조"),
    AFTERNOON("오후조"),
    CUSTOM("직접 입력");

    private final String description;
}

