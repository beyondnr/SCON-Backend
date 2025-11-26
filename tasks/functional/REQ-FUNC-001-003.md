# REQ-FUNC-001~003: 온보딩 및 매장/직원 관리 백엔드 API 구현

## 1. 목적 및 요약
* **목적**: 신규 사장님의 회원가입, 매장 생성, 직원 등록을 처리하는 백엔드 API를 구현한다.
* **요약**: `Owner`, `Store`, `Employee` 엔터티를 정의하고, 온보딩 마법사 및 설정 페이지에서 사용되는 CRUD API를 개발한다.

## 2. 관련 REQ 및 엔티티
* **관련 REQ**: `REQ-FUNC-001` (온보딩), `REQ-FUNC-002` (매장), `REQ-FUNC-003` (직원)
* **관련 엔티티**: `Owner` (User), `Store`, `Employee`

## 3. 주요 기능 및 처리 단계
1. **회원가입 (Owner Registration)**: 이메일/비밀번호(암호화) 저장, JWT 발급.
2. **매장 생성 (Create Store)**: Owner와 연관된 Store 레코드 생성. 업종/영업시간 정보 저장.
3. **직원 등록 (Create Employee)**: Store에 소속된 Employee 레코드 생성. 시급/계약형태 저장. PII(연락처) 암호화 처리.
4. **조회 및 수정**: 매장 정보 및 직원 목록 조회, 수정 API.

## 4. API 명세 (개요)
* `POST /api/v1/auth/register`: 사장님 가입.
* `POST /api/v1/stores`: 매장 생성.
* `POST /api/v1/stores/{storeId}/employees`: 직원 등록.
* `GET /api/v1/stores/{storeId}/employees`: 직원 목록 조회.
* `PUT /api/v1/employees/{employeeId}`: 직원 정보 수정.

---

```yaml
task_id: "REQ-FUNC-001-003"
title: "온보딩 및 매장/직원 관리 백엔드 API 구현"
summary: >
  사장님 회원가입, 매장 설정, 직원 등록을 위한 REST API와
  관련 엔터티(Store, Employee) 및 서비스 로직을 구현한다.
type: "functional"

epic: "E1_ONBOARDING"
req_ids: ["REQ-FUNC-001", "REQ-FUNC-002", "REQ-FUNC-003"]
component: ["backend.api", "user-service"]

context:
  srs_section: "4.1 Functional Requirements"
  entities: ["Owner", "Store", "Employee"]

inputs:
  description: >
    회원가입 정보, 매장 상세 정보, 직원 인적 사항 및 계약 정보.
  fields:
    - name: "owner_data"
      type: "object"
      validation: ["email_format", "password_policy"]
    - name: "store_data"
      type: "object"
      validation: ["name_required", "business_type_enum"]
    - name: "employee_data"
      type: "object"
      validation: ["hourly_wage > 0", "contact_info_format"]

outputs:
  description: >
    생성된 리소스 ID 및 상태.
  success:
    http_status: 201
    body: "{ id: 'UUID', ... }"
  failure_cases:
    - code: "DUPLICATE_EMAIL"
      http_status: 409

steps_hint:
  - "JPA Entity (Store, Employee) 및 Repository 설계."
  - "비밀번호 BCrypt 해싱 및 연락처 AES-256 암호화 적용 (Converter/Listener)."
  - "Service 레이어에서 트랜잭션 관리."
  - "Controller 및 DTO 매핑 구현."

preconditions:
  - "DB(MySQL) 스키마가 생성되어 있거나 ddl-auto 설정이 되어 있어야 한다."

postconditions:
  - "API를 통해 사장님, 매장, 직원이 정상적으로 생성되고 DB에 저장된다."
  - "PII(연락처)는 암호화되어 저장된다."

tests:
  unit:
    - "Employee 생성 시 시급/계약형태 유효성 검사 테스트"
  integration:
    - "회원가입 -> 매장생성 -> 직원등록 시나리오 통합 테스트"

dependencies: ["TASK-INFRA-DB-SETUP"]

parallelizable: true
estimated_effort: "M"
priority: "Must"
agent_profile: ["backend"]

required_tools:
  languages: ["Java"]
  frameworks: ["Spring Boot 3.x", "JPA"]
  infra: ["MySQL"]

references:
  srs: ["REQ-FUNC-001", "REQ-FUNC-002", "REQ-FUNC-003"]

risk_notes:
  - "PII 암호화 키 관리에 주의 (초기는 환경변수/설정파일 기반)."
  - "다중 매장 확장성을 고려해 Store-Owner 관계 설계."

example_commands:
  - "REQ-FUNC-001-003 Task에 따라 매장/직원 관리 API를 구현해줘."
```

