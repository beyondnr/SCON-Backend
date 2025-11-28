## 1. 목적
사장님 회원가입(온보딩), 매장 생성, 직원 관리를 위한 REST API를 구현한다.
SRS의 ST-Story-2 (1클릭 스케줄 승인)의 선행 요구사항을 충족한다.

## 2. 범위 (SRS Traceability)

| REQ ID | SRS 제목 | 설명 | Priority |
|--------|----------|------|----------|
| `REQ-FUNC-001` | 3단계 온보딩 마법사 | 회원가입 → 매장설정 → 첫 스케줄 생성 플로우 | Must |
| `REQ-FUNC-002` | 매장 정보 수집 | 매장 ID, 업종, 영업시간, 대리인 정보 저장 | Must |
| `REQ-FUNC-003` | 직원 등록 | 역할, 계약 유형, 시급, 연락처 등록 | Must |

**Story 매핑**: ST-Story-2 (1클릭 스케줄 승인)

---

## 3. 상세 작업

### 3.1 인프라/보안 (선행 작업)
- [ ] `.env.example`에 JWT/암호화 관련 환경변수 추가
- [ ] `EncryptionUtil.java` 구현 (AES-256-GCM)
- [ ] `JwtTokenProvider.java` 구현
- [ ] Spring Security 기본 설정 (`SecurityConfig.java`)

### 3.2 Owner 엔티티 확장
- [ ] `Owner.password` 필드 추가 (BCrypt 해시 저장)
- [ ] `OwnerRepository`에 인증 관련 메서드 추가

### 3.3 인증 API (REQ-FUNC-001)
- [ ] `AuthController.java` 구현
  - `POST /api/v1/auth/signup` - 회원가입
  - `POST /api/v1/auth/login` - 로그인 (JWT 발급)
  - `POST /api/v1/auth/refresh` - 토큰 갱신
- [ ] `AuthService.java` 구현
- [ ] `SignupRequestDto`, `LoginRequestDto`, `TokenResponseDto` 정의

### 3.4 매장 관리 API (REQ-FUNC-002)
- [ ] `StoreController.java` 구현
  - `POST /api/v1/stores` - 매장 생성
  - `GET /api/v1/stores/{id}` - 매장 조회
  - `GET /api/v1/stores` - 내 매장 목록 조회
  - `PUT /api/v1/stores/{id}` - 매장 정보 수정
- [ ] `StoreService.java` 구현
- [ ] `StoreRequestDto`, `StoreResponseDto` 정의

### 3.5 직원 관리 API (REQ-FUNC-003)
- [ ] `EmployeeController.java` 구현
  - `POST /api/v1/stores/{storeId}/employees` - 직원 등록
  - `GET /api/v1/stores/{storeId}/employees` - 직원 목록 조회
  - `GET /api/v1/employees/{id}` - 직원 상세 조회
  - `PUT /api/v1/employees/{id}` - 직원 정보 수정
  - `DELETE /api/v1/employees/{id}` - 직원 삭제 (Soft Delete)
- [ ] `EmployeeService.java` 구현 (PII 암호화 포함)
- [ ] `EmployeeRequestDto`, `EmployeeResponseDto` 정의

### 3.6 온보딩 플로우 API (REQ-FUNC-001 전체)
- [ ] `OnboardingController.java` 구현
  - `POST /api/v1/onboarding/complete` - 온보딩 완료 (첫 스케줄 Draft 생성)
- [ ] `OnboardingService.java` 구현

---

## 4. Acceptance Criteria (Given/When/Then)

### AC-001: 사장님 회원가입 및 JWT 발급 (REQ-FUNC-001)
```
Given 신규 사장님이 회원가입 페이지에 접속했을 때
When 이메일, 비밀번호, 이름, 전화번호를 입력하고 가입을 완료하면
Then
  - Owner 엔티티가 생성되어야 한다
  - 비밀번호는 BCrypt로 해시되어 저장되어야 한다
  - JWT Access Token과 Refresh Token이 발급되어야 한다
```

### AC-002: 사장님 로그인 (REQ-FUNC-001)
```
Given 가입된 사장님이 로그인 페이지에 접속했을 때
When 올바른 이메일과 비밀번호를 입력하면
Then
  - JWT Access Token (30분 만료)이 발급되어야 한다
  - JWT Refresh Token (7일 만료)이 발급되어야 한다
  - 응답에 ownerId가 포함되어야 한다
```

### AC-003: 매장 정보 저장 및 조회 (REQ-FUNC-002)
```
Given 인증된 사장님이 매장 설정 화면에서
When 필수 필드(매장명, 업종, 영업시간)를 입력하고 저장하면
Then
  - Store 엔티티가 Owner와 연결되어 저장되어야 한다
  - GET /api/v1/stores/{id} API로 재조회 가능해야 한다
  - 응답 시간이 p95 기준 0.8초 이하여야 한다 (REQ-NF-001)
```

### AC-004: 직원 등록 및 PII 암호화 (REQ-FUNC-003)
```
Given 사장님이 직원 추가 화면을 열고
When 필수 정보(이름, 계약유형, 시급, 연락처)를 입력·저장하면
Then
  - 고유한 Employee ID가 생성되어야 한다
  - Store와 FK로 연결되어야 한다
  - phone 필드는 DB에 AES-256-GCM으로 암호화되어 저장되어야 한다 (REQ-NF-007)
  - 조회 API 응답에서는 복호화된 값이 반환되어야 한다
```

### AC-005: 온보딩 완료 및 첫 스케줄 생성 (REQ-FUNC-001)
```
Given 신규 사장님이 회원가입 + 매장 설정을 완료했을 때
When 온보딩 완료 API를 호출하면
Then
  - 해당 매장에 현재 주차의 Draft 스케줄이 자동 생성되어야 한다
  - 대시보드에서 해당 스케줄을 조회/수정할 수 있어야 한다
  - (KPI 참고) 10분 이내 완료를 목표로 한다
```

---

## 5. 완료 조건 요약
- [ ] 신규 사장님이 가입 후 로그인하여 JWT를 발급받을 수 있다.
- [ ] 사장님이 매장을 생성하고 조회할 수 있다.
- [ ] 사장님이 매장에 직원을 등록할 수 있다.
- [ ] 직원의 개인정보(PII)는 DB에 AES-256으로 암호화되어 저장된다.
- [ ] 온보딩 완료 시 첫 스케줄(Draft)이 자동 생성된다.

---

## 6. NFR 추적성 (Non-Functional Requirements)

| NFR ID | 카테고리 | 설명 | 적용 범위 |
|--------|----------|------|-----------|
| `REQ-NF-001` | 성능 | API 응답 시간 p95 ≤ 0.8s | 매장/직원 조회 API |
| `REQ-NF-007` | 보안 | 저장 데이터 암호화 (AES-256) | Employee.phone 암호화 |
| `REQ-NF-008` | 보안 | 전송 구간 암호화 (TLS 1.3) | 모든 API 엔드포인트 |
| `REQ-NF-010` | 보안 | PII 최소 수집 (8개 이하) | Employee 필드 제한 |
| `REQ-NF-011` | 보안 | 관리자 접근 제어 (MFA) | v1.1+ 확장 대상 |

---

## 7. 보안 구현 요구사항

### 7.1 환경변수 관리

| 변수명 | 용도 | 보안 등급 |
|--------|------|-----------|
| `JWT_SECRET_KEY` | JWT 서명 키 | 🔴 Critical |
| `JWT_ACCESS_EXPIRATION` | Access Token 만료 (ms) | Normal |
| `JWT_REFRESH_EXPIRATION` | Refresh Token 만료 (ms) | Normal |
| `ENCRYPTION_KEY` | PII 암호화 키 | 🔴 Critical |

### 7.2 암호화 스펙

| 항목 | 스펙 |
|------|------|
| **비밀번호 해시** | BCrypt (cost factor: 12) |
| **PII 암호화** | AES-256-GCM |
| **대상 필드** | `Employee.phone` |

### 7.3 JWT 스펙

| 항목 | 스펙 |
|------|------|
| **알고리즘** | HS256 (대칭키) |
| **Access Token 만료** | 30분 |
| **Refresh Token 만료** | 7일 |

---

## 8. 테스트 케이스 추적성

| AC | Test Case ID | 테스트 유형 | 설명 |
|----|--------------|-------------|------|
| AC-001 | TC-AUTH-001 | 통합 | 회원가입 성공 |
| AC-001 | TC-AUTH-002 | 단위 | 비밀번호 BCrypt 해시 검증 |
| AC-002 | TC-AUTH-004 | 통합 | 로그인 성공 + JWT 발급 |
| AC-003 | TC-STORE-001 | 통합 | 매장 생성 API |
| AC-003 | TC-STORE-003 | 성능 | 매장 조회 p95 ≤ 0.8s |
| AC-004 | TC-EMP-001 | 통합 | 직원 등록 API |
| AC-004 | TC-EMP-002 | 보안 | PII 암호화 저장 검증 |
| AC-005 | TC-ONBOARD-001 | E2E | 온보딩 플로우 전체 |

---

## 9. 선행 조건 (Dependencies)

- [x] SCON-ENV-001: 프로젝트 초기화 완료
- [x] SCON-ENV-002: 데이터베이스 스키마 완료 (Owner, Store, Employee 엔티티)
