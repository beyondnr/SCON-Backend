-- 기존 데이터 변환
UPDATE employees 
SET employment_type = 'MANAGER' 
WHERE employment_type = 'FULL_TIME';

UPDATE employees 
SET employment_type = 'EMPLOYEE' 
WHERE employment_type = 'PART_TIME';

-- 데이터 확인 쿼리
SELECT employment_type, COUNT(*) as count
FROM employees
GROUP BY employment_type;

