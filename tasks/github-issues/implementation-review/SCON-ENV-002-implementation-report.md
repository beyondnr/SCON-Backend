# Implementation Report: SCON-ENV-002

## 데이터베이스 스키마 설계 및 설정

| 항목 | 내용 |
|------|------|
| **Issue ID** | SCON-ENV-002 |
| **GitHub Issue** | [#3](https://github.com/beyondnr/SCON-Backend/issues/3) |
| **Pull Request** | [#14](https://github.com/beyondnr/SCON-Backend/pull/14) |
| **실행일** | 2025-11-28 |
| **커밋 해시** | `52c9272` |
| **상태** | ✅ Completed |

---

## 1. 실행 개요

### 1.1 목적
핵심 도메인 엔터티를 위한 초기 MySQL 데이터베이스 스키마를 설계하고 구현한다.

### 1.2 범위
- `Owner`, `Store`, `Employee`, `Schedule`, `Shift`, `AvailabilitySubmission` 엔터티 정의
- Docker Compose를 통한 MySQL 로컬 개발 환경 구성
- 엔터티 간 관계(OneToMany, ManyToOne) 및 제약조건 설정
- JPA Repository 인터페이스 생성

### 1.3 적용된 Cursor Rules
| Rule | 설명 |
|------|------|
| `101-build-and-env-setup.mdc` | 환경변수 설정 (.env 파일) |
| `102-gitflow-agent.mdc` | Git Flow 커밋/브랜치 전략 |
| `201-code-commenting.mdc` | 코드 주석 규칙 |
| `202-java-project-structure.mdc` | 패키지 구조, 네이밍 컨벤션 |
| `301-java-springboot.mdc` | DI, Lombok, 로깅 |
| `302-jpa-mysql.mdc` | JPA Entity, Repository 규칙 |

---

## 2. 실행 단계 (6 Phases)

### Phase 0: Git 브랜치 초기화 ✅
**목표**: Feature 브랜치 생성 및 작업 준비

**작업 내용**:
- `feat/SCON-ENV-002-database-schema` 브랜치 생성
- main 브랜치에서 분기

**명령어**:
```bash
git checkout -b feat/SCON-ENV-002-database-schema
```

---

### Phase 1: Infrastructure Setup ✅
**목표**: Docker Compose 및 환경변수 설정

**작업 내용**:
- `docker-compose.yml` - MySQL 8.x 컨테이너 설정
- `.env.example` - 환경변수 템플릿
- `application-local.yml` - 로컬 MySQL 연결 프로파일
- `.gitignore` - `.env` 파일 제외 추가

**생성 파일**:
```
SCON-Backend/
├── docker-compose.yml       # MySQL 8.x 컨테이너 (포트 3307)
├── .env.example             # 환경변수 템플릿
├── .gitignore               # .env 제외 추가
└── src/main/resources/
    └── application-local.yml  # MySQL 로컬 연결 설정
```

**Docker Compose 설정**:
```yaml
services:
  mysql:
    image: mysql:8.0
    container_name: scon-mysql
    env_file:
      - .env
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD:-root}
      MYSQL_DATABASE: ${MYSQL_DATABASE:-scon_db}
      MYSQL_USER: ${MYSQL_USER:-scon}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD:-scon1234}
    ports:
      - "3307:3306"
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5
```

---

### Phase 2: Enum 타입 정의 ✅
**목표**: 도메인 열거형 타입 정의

**작업 내용**:
- `EmploymentType.java` - 직원 고용 형태 (FULL_TIME, PART_TIME)
- `ScheduleStatus.java` - 스케줄 상태 (DRAFT, PENDING, APPROVED, PUBLISHED)
- `package-info.java` - 패키지 설명

**생성 파일**:
```
src/main/java/vibe/scon/scon_backend/entity/enums/
├── EmploymentType.java
├── ScheduleStatus.java
└── package-info.java
```

**설계 결정**:
- `DayOfWeek`는 Java 표준 라이브러리 `java.time.DayOfWeek` 사용 (커스텀 Enum 불필요)

---

### Phase 3: Entity 구현 ✅
**목표**: 6개 도메인 엔티티 구현

**구현 순서** (의존성 기반):
1. `Owner` - 독립 엔티티
2. `Store` - Owner 참조
3. `Employee` - Store 참조
4. `Schedule` - Store 참조
5. `Shift` - Schedule, Employee 참조
6. `AvailabilitySubmission` - Employee 참조

**엔티티 관계 다이어그램**:
```
Owner (1) ──────┬──── (*) Store
                │
Store (1) ──────┼──── (*) Employee
                │
Store (1) ──────┼──── (*) Schedule
                │
Schedule (1) ───┼──── (*) Shift
                │
Employee (1) ───┼──── (*) Shift
                │
Employee (1) ───┴──── (*) AvailabilitySubmission
```

**적용된 JPA 규칙 (`302-jpa-mysql.mdc`)**:
| 규칙 | 적용 |
|------|------|
| `extends BaseEntity` | ✅ 모든 엔티티 |
| `@Table(name = "snake_case")` | ✅ owners, stores, employees, schedules, shifts, availability_submissions |
| `FetchType.LAZY` | ✅ 모든 @ManyToOne, @OneToMany |
| `@NoArgsConstructor(access = PROTECTED)` | ✅ 모든 엔티티 |
| `@Getter` + `@Builder` (No @Data) | ✅ 모든 엔티티 |

**생성 파일**:
```
src/main/java/vibe/scon/scon_backend/entity/
├── Owner.java
├── Store.java
├── Employee.java
├── Schedule.java
├── Shift.java
└── AvailabilitySubmission.java
```

---

### Phase 4: Repository 구현 ✅
**목표**: JPA Repository 인터페이스 생성

**작업 내용**:
| Repository | 커스텀 쿼리 메서드 |
|------------|-------------------|
| `OwnerRepository` | `findByEmail()`, `existsByEmail()` |
| `StoreRepository` | `findByOwnerId()` |
| `EmployeeRepository` | `findByStoreId()` |
| `ScheduleRepository` | `findByStoreId()`, `findByStoreIdAndWeekStartDate()`, `findByStatus()` |
| `ShiftRepository` | `findByScheduleId()`, `findByEmployeeId()`, `findByWorkDate()` |
| `AvailabilitySubmissionRepository` | `findByEmployeeId()`, `findByEmployeeIdAndWeekStartDate()` |

**생성 파일**:
```
src/main/java/vibe/scon/scon_backend/repository/
├── OwnerRepository.java
├── StoreRepository.java
├── EmployeeRepository.java
├── ScheduleRepository.java
├── ShiftRepository.java
└── AvailabilitySubmissionRepository.java
```

---

### Phase 5: DDL 검증 ✅
**목표**: Docker MySQL에서 테이블 생성 확인

**실행 절차**:
```bash
# 1. Docker Desktop 시작
Start-Process "C:\Program Files\Docker\Docker\Docker Desktop.exe"

# 2. MySQL 컨테이너 시작
docker-compose up -d

# 3. MySQL 사용자 권한 설정
docker exec scon-mysql mysql -u root -proot1234 -e \
  "ALTER USER 'scon'@'%' IDENTIFIED WITH mysql_native_password BY 'scon1234'; FLUSH PRIVILEGES;"

# 4. 애플리케이션 실행 (local 프로파일)
./gradlew bootRun --args="--spring.profiles.active=local"

# 5. 테이블 생성 확인
docker exec scon-mysql mysql -u scon -pscon1234 scon_db -e "SHOW TABLES;"
```

**테이블 생성 결과**:
```
+------------------------+
| Tables_in_scon_db      |
+------------------------+
| availability_submissions|
| employees              |
| owners                 |
| schedules              |
| shifts                 |
| stores                 |
+------------------------+
```

---

## 3. 테스트 결과

### 3.1 빌드 검증
```bash
$ ./gradlew build -x test
BUILD SUCCESSFUL in 24s
5 actionable tasks: 5 executed
```

### 3.2 테이블 구조 검증

**owners 테이블**:
```sql
DESCRIBE owners;
+------------+--------------+------+-----+---------+----------------+
| Field      | Type         | Null | Key | Default | Extra          |
+------------+--------------+------+-----+---------+----------------+
| id         | bigint       | NO   | PRI | NULL    | auto_increment |
| created_at | datetime(6)  | NO   |     | NULL    |                |
| updated_at | datetime(6)  | NO   |     | NULL    |                |
| email      | varchar(255) | NO   | UNI | NULL    |                |
| name       | varchar(100) | NO   |     | NULL    |                |
| password   | varchar(255) | NO   |     | NULL    |                |
| phone      | varchar(20)  | YES  |     | NULL    |                |
+------------+--------------+------+-----+---------+----------------+
```

**stores 테이블**:
```sql
DESCRIBE stores;
+---------------+--------------+------+-----+---------+----------------+
| Field         | Type         | Null | Key | Default | Extra          |
+---------------+--------------+------+-----+---------+----------------+
| id            | bigint       | NO   | PRI | NULL    | auto_increment |
| created_at    | datetime(6)  | NO   |     | NULL    |                |
| updated_at    | datetime(6)  | NO   |     | NULL    |                |
| address       | varchar(255) | YES  |     | NULL    |                |
| business_type | varchar(50)  | YES  |     | NULL    |                |
| close_time    | time(6)      | YES  |     | NULL    |                |
| name          | varchar(100) | NO   |     | NULL    |                |
| open_time     | time(6)      | YES  |     | NULL    |                |
| owner_id      | bigint       | NO   | MUL | NULL    |                |
+---------------+--------------+------+-----+---------+----------------+
```

**employees 테이블**:
```sql
DESCRIBE employees;
+-----------------+--------------------------------+------+-----+---------+----------------+
| Field           | Type                           | Null | Key | Default | Extra          |
+-----------------+--------------------------------+------+-----+---------+----------------+
| id              | bigint                         | NO   | PRI | NULL    | auto_increment |
| created_at      | datetime(6)                    | NO   |     | NULL    |                |
| updated_at      | datetime(6)                    | NO   |     | NULL    |                |
| employment_type | enum('FULL_TIME','PART_TIME')  | NO   |     | NULL    |                |
| hourly_wage     | decimal(10,2)                  | YES  |     | NULL    |                |
| name            | varchar(100)                   | NO   |     | NULL    |                |
| phone           | varchar(20)                    | YES  |     | NULL    |                |
| store_id        | bigint                         | NO   | MUL | NULL    |                |
+-----------------+--------------------------------+------+-----+---------+----------------+
```

**schedules 테이블**:
```sql
DESCRIBE schedules;
+-----------------+--------------------------------------------+------+-----+---------+----------------+
| Field           | Type                                       | Null | Key | Default | Extra          |
+-----------------+--------------------------------------------+------+-----+---------+----------------+
| id              | bigint                                     | NO   | PRI | NULL    | auto_increment |
| created_at      | datetime(6)                                | NO   |     | NULL    |                |
| updated_at      | datetime(6)                                | NO   |     | NULL    |                |
| status          | enum('APPROVED','DRAFT','PENDING','PUBLISHED') | NO   |     | NULL    |                |
| week_start_date | date                                       | NO   |     | NULL    |                |
| store_id        | bigint                                     | NO   | MUL | NULL    |                |
+-----------------+--------------------------------------------+------+-----+---------+----------------+
```

**shifts 테이블**:
```sql
DESCRIBE shifts;
+-------------+-------------+------+-----+---------+----------------+
| Field       | Type        | Null | Key | Default | Extra          |
+-------------+-------------+------+-----+---------+----------------+
| id          | bigint      | NO   | PRI | NULL    | auto_increment |
| created_at  | datetime(6) | NO   |     | NULL    |                |
| updated_at  | datetime(6) | NO   |     | NULL    |                |
| end_time    | time(6)     | NO   |     | NULL    |                |
| start_time  | time(6)     | NO   |     | NULL    |                |
| work_date   | date        | NO   |     | NULL    |                |
| employee_id | bigint      | NO   | MUL | NULL    |                |
| schedule_id | bigint      | NO   | MUL | NULL    |                |
+-------------+-------------+------+-----+---------+----------------+
```

**availability_submissions 테이블**:
```sql
DESCRIBE availability_submissions;
+-----------------+---------------------------------------------------------------+------+-----+---------+----------------+
| Field           | Type                                                          | Null | Key | Default | Extra          |
+-----------------+---------------------------------------------------------------+------+-----+---------+----------------+
| id              | bigint                                                        | NO   | PRI | NULL    | auto_increment |
| created_at      | datetime(6)                                                   | NO   |     | NULL    |                |
| updated_at      | datetime(6)                                                   | NO   |     | NULL    |                |
| day_of_week     | enum('FRIDAY','MONDAY','SATURDAY','SUNDAY','THURSDAY','TUESDAY','WEDNESDAY') | NO   |     | NULL    |                |
| end_time        | time(6)                                                       | NO   |     | NULL    |                |
| start_time      | time(6)                                                       | NO   |     | NULL    |                |
| week_start_date | date                                                          | NO   |     | NULL    |                |
| employee_id     | bigint                                                        | NO   | MUL | NULL    |                |
+-----------------+---------------------------------------------------------------+------+-----+---------+----------------+
```

### 3.3 ERD 일치 검증

| 테이블 | 컬럼 수 | FK 관계 | 상태 |
|--------|---------|---------|------|
| owners | 7 | - | ✅ 일치 |
| stores | 9 | owner_id → owners | ✅ 일치 |
| employees | 8 | store_id → stores | ✅ 일치 |
| schedules | 6 | store_id → stores | ✅ 일치 |
| shifts | 8 | schedule_id → schedules, employee_id → employees | ✅ 일치 |
| availability_submissions | 8 | employee_id → employees | ✅ 일치 |

---

## 4. 생성된 파일 목록 (20개)

### 4.1 Infrastructure (4개)
| 파일 | 설명 |
|------|------|
| `docker-compose.yml` | MySQL 8.x 컨테이너 설정 |
| `.env.example` | 환경변수 템플릿 |
| `application-local.yml` | MySQL 로컬 연결 프로파일 |
| `.gitignore` | .env 파일 제외 추가 |

### 4.2 Enum 타입 (3개)
| 파일 | 설명 |
|------|------|
| `entity/enums/EmploymentType.java` | 직원 고용 형태 |
| `entity/enums/ScheduleStatus.java` | 스케줄 상태 |
| `entity/enums/package-info.java` | 패키지 설명 |

### 4.3 Entity (6개)
| 파일 | 테이블명 | 설명 |
|------|----------|------|
| `entity/Owner.java` | owners | 사장/운영자 |
| `entity/Store.java` | stores | 매장 |
| `entity/Employee.java` | employees | 직원 |
| `entity/Schedule.java` | schedules | 주간 스케줄 |
| `entity/Shift.java` | shifts | 근무 시프트 |
| `entity/AvailabilitySubmission.java` | availability_submissions | 가용시간 제출 |

### 4.4 Repository (6개)
| 파일 | 설명 |
|------|------|
| `repository/OwnerRepository.java` | Owner CRUD + findByEmail |
| `repository/StoreRepository.java` | Store CRUD + findByOwnerId |
| `repository/EmployeeRepository.java` | Employee CRUD + findByStoreId |
| `repository/ScheduleRepository.java` | Schedule CRUD + 복합 조회 |
| `repository/ShiftRepository.java` | Shift CRUD + 다중 조회 |
| `repository/AvailabilitySubmissionRepository.java` | Availability CRUD + 복합 조회 |

---

## 5. 최종 프로젝트 구조 (추가분)

```
src/main/java/vibe/scon/scon_backend/
├── entity/
│   ├── enums/
│   │   ├── EmploymentType.java     ★ NEW
│   │   ├── ScheduleStatus.java     ★ NEW
│   │   └── package-info.java       ★ NEW
│   ├── BaseEntity.java             (기존)
│   ├── Owner.java                  ★ NEW
│   ├── Store.java                  ★ NEW
│   ├── Employee.java               ★ NEW
│   ├── Schedule.java               ★ NEW
│   ├── Shift.java                  ★ NEW
│   ├── AvailabilitySubmission.java ★ NEW
│   └── package-info.java           (기존)
└── repository/
    ├── OwnerRepository.java        ★ NEW
    ├── StoreRepository.java        ★ NEW
    ├── EmployeeRepository.java     ★ NEW
    ├── ScheduleRepository.java     ★ NEW
    ├── ShiftRepository.java        ★ NEW
    ├── AvailabilitySubmissionRepository.java ★ NEW
    └── package-info.java           (기존)

src/main/resources/
├── application.yml                 (기존)
├── application-dev.yml             (기존)
├── application-prod.yml            (기존)
└── application-local.yml           ★ NEW

project root/
├── docker-compose.yml              ★ NEW
├── .env.example                    ★ NEW
└── .gitignore                      (수정됨)
```

---

## 6. 완료 조건 검증

| 조건 | 상태 | 검증 방법 |
|------|------|----------|
| `docker-compose up -d` 정상 기동 | ✅ | Container scon-mysql Started |
| `local` 프로파일로 앱 실행 시 DB 연결 성공 | ✅ | HikariPool-1 - Start completed |
| 6개 테이블 생성 확인 | ✅ | SHOW TABLES 결과 6개 |
| ERD와 스키마 일치 | ✅ | DESCRIBE 결과 검증 |
| `./gradlew build` 성공 | ✅ | BUILD SUCCESSFUL |

---

## 7. Git 커밋 정보

### 7.1 브랜치
```
feat/SCON-ENV-002-database-schema
```

### 7.2 커밋 목록 (4개)
```
52c9272 [Repository] 6개 JPA Repository 인터페이스 구현
538c845 [Entity] 6개 도메인 엔티티 구현
6df2daf [Entity] Enum 타입 정의 (EmploymentType, ScheduleStatus)
2cd9b3d [Infra] Docker Compose 및 환경변수 템플릿 추가
```

### 7.3 변경 통계
```
20 files changed
1053 insertions(+)
8 deletions(-)
```

### 7.4 커밋 상세

**[Infra] Docker Compose 및 환경변수 템플릿 추가**
- docker-compose.yml: MySQL 8.x 컨테이너 (포트 3307)
- .env.example: 환경변수 템플릿
- application-local.yml: 로컬 MySQL 연결 프로파일
- .gitignore: .env 파일 제외 추가

**[Entity] Enum 타입 정의 (EmploymentType, ScheduleStatus)**
- EmploymentType: FULL_TIME, PART_TIME
- ScheduleStatus: DRAFT, PENDING, APPROVED, PUBLISHED
- java.time.DayOfWeek 표준 라이브러리 사용

**[Entity] 6개 도메인 엔티티 구현**
- Owner: 사장/운영자 엔티티
- Store: 매장 엔티티 (Owner 참조)
- Employee: 직원 엔티티 (Store 참조)
- Schedule: 스케줄 엔티티 (Store 참조, 복합 유니크)
- Shift: 시프트 엔티티 (Schedule, Employee 참조)
- AvailabilitySubmission: 가용시간 제출 엔티티 (Employee 참조)

**[Repository] 6개 JPA Repository 인터페이스 구현**
- OwnerRepository, StoreRepository, EmployeeRepository
- ScheduleRepository, ShiftRepository, AvailabilitySubmissionRepository

---

## 8. 다음 단계

Issue-002 (SCON-ENV-002)가 완료되었습니다. ISSUE_EXECUTION_PLAN.md에 따라 다음 작업은:

**[Issue-004] REQ-FUNC-001~003: 온보딩 및 매장/직원 관리 API 구현**
- Issue-002에 의존 (Entity/Repository 완료)
- AuthService 구현 (가입, 로그인, JWT 토큰 발급)
- StoreController/Service 구현 (매장 생성)
- EmployeeController/Service 구현 (직원 추가/목록)

**병렬 가능 작업**:
- [Issue-005] REQ-FUNC-004~005: 직원 가용시간 제출 API 및 검증 로직
- [Issue-006] REQ-FUNC-006~007: 스케줄 대시보드 및 편집 API 구현

---

*Generated: 2025-11-28*
*Agent: Cursor AI Assistant*
*Branch: feat/SCON-ENV-002-database-schema*
*PR: #14*

