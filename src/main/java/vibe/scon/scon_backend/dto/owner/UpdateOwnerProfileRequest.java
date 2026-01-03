package vibe.scon.scon_backend.dto.owner;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 사용자 프로필 수정 요청 DTO.
 * 
 * <p>마이페이지에서 사용자 프로필 정보를 수정할 때 사용합니다.</p>
 * 
 * <h3>요구사항 추적 (Traceability):</h3>
 * <ul>
 *   <li>{@code POC-BE-FUNC-002} - 마이페이지 사용자 프로필 조회/수정 API</li>
 * </ul>
 * 
 * <h3>필드 제약사항:</h3>
 * <ul>
 *   <li>{@code name}: 선택 필드이지만, 제공된 경우 빈 문자열은 허용하지 않음 (DB 제약: nullable=false)</li>
 *   <li>{@code phone}: 선택 필드이며 null 또는 빈 문자열 허용 (nullable 필드)</li>
 * </ul>
 * 
 * @see <a href="../../../SCON-Update-Plan/POC-BE-FUNC-002-mypage.md">POC-BE-FUNC-002</a>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOwnerProfileRequest {

    /**
     * 이름 (최대 100자).
     * 
     * <p>제공된 경우 빈 문자열은 허용하지 않습니다. 실제 유효성 검증은 Service 레이어에서 수행됩니다.</p>
     */
    @Size(max = 100, message = "이름은 최대 100자까지 입력 가능합니다")
    private String name;

    /**
     * 휴대폰 번호 (최대 20자).
     * 
     * <p>선택 필드이며 null 또는 빈 문자열 허용합니다. 빈 문자열은 null로 처리됩니다.</p>
     */
    @Size(max = 20, message = "휴대폰 번호는 최대 20자까지 입력 가능합니다")
    private String phone;
}

