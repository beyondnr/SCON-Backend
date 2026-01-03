package vibe.scon.scon_backend.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

/**
 * '강누리' 직원의 소유 계정 확인 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("'강누리' 직원 소유 계정 확인")
class CheckEmployeeOwnerTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("'강누리' 직원의 소유 계정 정보 조회")
    void checkEmployeeOwner() {
        System.out.println("\n========================================");
        System.out.println("'강누리' 직원 소유 계정 확인");
        System.out.println("========================================\n");

        try {
            // 먼저 테이블이 존재하는지 확인
            String checkTableSql = """
                SELECT COUNT(*) as cnt FROM information_schema.tables 
                WHERE table_name = 'EMPLOYEES'
                """;
            
            try {
                jdbcTemplate.queryForList(checkTableSql);
            } catch (Exception e) {
                // H2에서는 다른 방식으로 확인
            }

            String sql = """
                SELECT 
                    e.id AS employee_id,
                    e.name AS employee_name,
                    s.id AS store_id,
                    s.name AS store_name,
                    o.id AS owner_id,
                    o.email AS owner_email,
                    o.name AS owner_name,
                    e.created_at AS employee_created_at
                FROM employees e
                INNER JOIN stores s ON e.store_id = s.id
                INNER JOIN owners o ON s.owner_id = o.id
                WHERE e.name = '강누리'
                """;

            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);

            if (results.isEmpty()) {
                System.out.println("⚠️ '강누리' 이름의 직원을 찾을 수 없습니다.\n");
                
                // 전체 직원 목록 확인
                String allEmployeesSql = """
                    SELECT 
                        e.name AS employee_name,
                        s.name AS store_name,
                        o.email AS owner_email,
                        o.name AS owner_name
                    FROM employees e
                    INNER JOIN stores s ON e.store_id = s.id
                    INNER JOIN owners o ON s.owner_id = o.id
                    ORDER BY e.created_at DESC
                    """;
                
                List<Map<String, Object>> allEmployees = jdbcTemplate.queryForList(allEmployeesSql);
                System.out.println("📋 전체 직원 목록:");
                if (allEmployees.isEmpty()) {
                    System.out.println("   데이터베이스에 직원 데이터가 없습니다.");
                    System.out.println("   (테스트 환경은 매번 초기화되므로 실제 데이터를 확인하려면");
                    System.out.println("    실행 중인 서버의 H2 콘솔(http://localhost:8080/h2-console)을 사용하세요.)");
                } else {
                    allEmployees.forEach(emp -> {
                        System.out.println(String.format("   - 직원: %s | 매장: %s | 소유자: %s (%s)", 
                            emp.get("employee_name"),
                            emp.get("store_name"),
                            emp.get("owner_name"),
                            emp.get("owner_email")));
                    });
                }
            } else {
                System.out.println("✅ '강누리' 직원 정보를 찾았습니다:\n");
                results.forEach(result -> {
                    System.out.println(String.format("   직원 ID: %s", result.get("employee_id")));
                    System.out.println(String.format("   직원 이름: %s", result.get("employee_name")));
                    System.out.println(String.format("   매장 ID: %s", result.get("store_id")));
                    System.out.println(String.format("   매장 이름: %s", result.get("store_name")));
                    System.out.println(String.format("   소유자 ID: %s", result.get("owner_id")));
                    System.out.println(String.format("   소유자 이메일: %s", result.get("owner_email")));
                    System.out.println(String.format("   소유자 이름: %s", result.get("owner_name")));
                    System.out.println(String.format("   생성일시: %s", result.get("employee_created_at")));
                    System.out.println(String.format("\n   → '강누리' 직원은 '%s' 계정이 소유한 매장에 속해 있습니다.", 
                        result.get("owner_email")));
                    System.out.println("----------------------------------------\n");
                });
            }

            System.out.println("========================================\n");
        } catch (Exception e) {
            System.err.println("❌ 데이터베이스 조회 중 오류 발생: " + e.getMessage());
            System.err.println("\n💡 실제 데이터를 확인하려면:");
            System.err.println("   1. 서버를 실행합니다 (./gradlew bootRun)");
            System.err.println("   2. http://localhost:8080/h2-console 접속");
            System.err.println("   3. JDBC URL: jdbc:h2:mem:scon_dev");
            System.err.println("   4. 다음 SQL 실행:");
            System.err.println("      SELECT e.name, o.email, o.name FROM employees e");
            System.err.println("      JOIN stores s ON e.store_id = s.id");
            System.err.println("      JOIN owners o ON s.owner_id = o.id");
            System.err.println("      WHERE e.name = '강누리';");
            e.printStackTrace();
        }
    }
}
