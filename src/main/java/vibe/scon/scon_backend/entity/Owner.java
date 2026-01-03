package vibe.scon.scon_backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 매장 사장/운영자 엔티티.
 * 
 * <p>시스템의 주요 사용자로서 매장을 소유하고 관리합니다.
 * 한 명의 사장은 여러 매장을 소유할 수 있습니다.</p>
 * 
 * <h3>테이블 정보:</h3>
 * <ul>
 *   <li>테이블명: {@code owners}</li>
 *   <li>기본키: {@code id} (AUTO_INCREMENT)</li>
 *   <li>유니크 제약: {@code email}</li>
 * </ul>
 * 
 * <h3>요구사항 추적 (Traceability):</h3>
 * <ul>
 *   <li>{@code REQ-FUNC-001} - 3단계 온보딩 마법사 (회원가입)</li>
 *   <li>{@code SRS §6.2.0} - ERD: OWNER 엔티티</li>
 * </ul>
 * 
 * @see Store
 * @see <a href="docs/GPT-SRS_v0.2.md">SRS §6.2.0 ERD</a>
 * @see <a href="tasks/github-issues/issue-003-REQ-FUNC-001-003.md">Issue-003: 온보딩 API</a>
 */
@Entity
@Table(name = "owners")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Owner extends BaseEntity {

    /**
     * 로그인용 이메일 주소 (유니크)
     */
    @Column(nullable = false, unique = true)
    private String email;

    /**
     * 암호화된 비밀번호
     */
    @Column(nullable = false)
    private String password;

    /**
     * 사장님 이름
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * 연락처 (선택)
     */
    @Column(length = 20)
    private String phone;

    /**
     * 서비스 이용약관 및 개인정보 처리방침 동의 여부.
     */
    @Column(name = "agreed_to_terms")
    private Boolean agreedToTerms;

    /**
     * 약관 동의 시점.
     */
    @Column(name = "agreed_at")
    private LocalDateTime agreedAt;

    /**
     * 소유한 매장 목록 (양방향 관계)
     */
    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Store> stores = new ArrayList<>();

    /**
     * Owner 엔티티 생성자
     * 
     * @param email 이메일 주소
     * @param password 암호화된 비밀번호
     * @param name 사장님 이름
     * @param phone 연락처 (선택)
     * @param agreedToTerms 약관 동의 여부
     * @param agreedAt 약관 동의 시점
     */
    @Builder
    public Owner(String email, String password, String name, String phone, Boolean agreedToTerms, LocalDateTime agreedAt) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.phone = phone;
        this.agreedToTerms = agreedToTerms;
        this.agreedAt = agreedAt;
    }

    /**
     * 매장 추가 연관관계 편의 메서드
     * 
     * @param store 추가할 매장
     */
    public void addStore(Store store) {
        stores.add(store);
        store.setOwner(this);
    }

    /**
     * 이름 업데이트
     * 
     * <p>사장님 이름을 업데이트합니다. 이름은 필수 필드이므로 null 또는 빈 문자열은 허용하지 않습니다.</p>
     * 
     * <h3>요구사항 추적 (Traceability):</h3>
     * <ul>
     *   <li>{@code POC-BE-FUNC-002} - 마이페이지 사용자 프로필 수정 API</li>
     * </ul>
     * 
     * @param name 새로운 이름 (null 또는 빈 문자열 불가)
     * @throws IllegalArgumentException name이 null이거나 빈 문자열인 경우
     * @see <a href="../../SCON-Update-Plan/POC-BE-FUNC-002-mypage.md">POC-BE-FUNC-002</a>
     */
    public void updateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("이름은 필수입니다");
        }
        this.name = name.trim();
    }

    /**
     * 전화번호 업데이트 (nullable)
     * 
     * <p>연락처를 업데이트합니다. 전화번호는 선택 필드이므로 null을 허용합니다.</p>
     * 
     * <h3>요구사항 추적 (Traceability):</h3>
     * <ul>
     *   <li>{@code POC-BE-FUNC-002} - 마이페이지 사용자 프로필 수정 API</li>
     * </ul>
     * 
     * @param phone 새로운 전화번호 (null 허용, 빈 문자열은 null로 처리)
     * @see <a href="../../SCON-Update-Plan/POC-BE-FUNC-002-mypage.md">POC-BE-FUNC-002</a>
     */
    public void updatePhone(String phone) {
        this.phone = (phone != null && !phone.trim().isEmpty()) ? phone.trim() : null;
    }

    /**
     * 로그인 실패 횟수.
     * 
     * <p>연속된 로그인 실패 횟수를 추적합니다. 5회 실패 시 계정이 잠깁니다.</p>
     * 
     * <h3>요구사항 추적 (Traceability):</h3>
     * <ul>
     *   <li>{@code POC-BE-SEC-002} - 백엔드 보안 강화 (계정 잠금 기능)</li>
     * </ul>
     * 
     * @see <a href="../../SCON-Update-Plan/POC-BE-SEC-002.md">POC-BE-SEC-002</a>
     */
    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts = 0;

    /**
     * 계정 잠금 해제 시간.
     * 
     * <p>계정이 잠긴 경우, 이 시간 이후에 잠금이 해제됩니다.</p>
     * 
     * <h3>요구사항 추적 (Traceability):</h3>
     * <ul>
     *   <li>{@code POC-BE-SEC-002} - 백엔드 보안 강화 (계정 잠금 기능)</li>
     * </ul>
     * 
     * @see <a href="../../SCON-Update-Plan/POC-BE-SEC-002.md">POC-BE-SEC-002</a>
     */
    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    /**
     * 계정 잠금.
     * 
     * <p>지정된 시간(분) 동안 계정을 잠급니다.</p>
     * 
     * @param minutes 잠금 시간 (분)
     */
    public void lockAccount(int minutes) {
        this.lockedUntil = LocalDateTime.now().plusMinutes(minutes);
    }

    /**
     * 로그인 실패 횟수 증가.
     */
    public void incrementFailedAttempts() {
        this.failedLoginAttempts++;
    }

    /**
     * 로그인 실패 횟수 및 잠금 상태 리셋.
     * 
     * <p>로그인 성공 시 호출됩니다.</p>
     */
    public void resetFailedAttempts() {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
    }

    /**
     * 계정이 잠겨있는지 확인.
     * 
     * @return 잠겨있으면 true, 그렇지 않으면 false
     */
    public boolean isLocked() {
        return lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now());
    }
}

