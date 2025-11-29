package vibe.scon.scon_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vibe.scon.scon_backend.entity.Employee;

import java.util.List;

/**
 * Employee 엔티티를 위한 JPA Repository.
 * 
 * <p>직원 데이터에 대한 기본 CRUD 및 커스텀 쿼리를 제공합니다.</p>
 * 
 * <h3>요구사항 추적 (Traceability):</h3>
 * <ul>
 *   <li>{@code REQ-FUNC-003} - 직원 등록 (CRUD 쿼리)</li>
 *   <li>{@code TC-EMP-001~008} - 직원 API 테스트</li>
 * </ul>
 * 
 * @see Employee
 * @see <a href="tasks/github-issues/issue-003-REQ-FUNC-001-003.md">Issue-003: 직원 API §8.3</a>
 */
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    
    /**
     * 특정 매장에 소속된 직원 목록 조회
     * 
     * @param storeId Store ID
     * @return 직원 목록
     */
    List<Employee> findByStoreId(Long storeId);
}

