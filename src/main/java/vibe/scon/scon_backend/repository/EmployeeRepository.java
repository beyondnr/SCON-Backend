package vibe.scon.scon_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vibe.scon.scon_backend.entity.Employee;

import java.util.List;

/**
 * Employee 엔티티를 위한 JPA Repository.
 * 
 * <p>직원 데이터에 대한 기본 CRUD 및 커스텀 쿼리를 제공합니다.</p>
 * 
 * @see Employee
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

