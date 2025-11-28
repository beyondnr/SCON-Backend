package vibe.scon.scon_backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
 * @see Store
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
     */
    @Builder
    public Owner(String email, String password, String name, String phone) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.phone = phone;
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
}

