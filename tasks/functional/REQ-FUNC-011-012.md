# REQ-FUNC-011~012: 급여 계산 엔진 및 미리보기 구현

## 1. 목적 및 요약
* **목적**: 승인된 스케줄 데이터를 바탕으로 직원별 예상 급여와 각종 수당을 자동으로 계산하고, 이를 조회하는 API를 구현한다.
* **요약**: `PayrollCalculator` 로직 구현(기본급, 주휴, 연장, 야간, 휴일) 및 `PayrollRecord` 엔터티 생성/조회 기능을 개발한다.

## 2. 관련 REQ 및 엔티티
* **관련 REQ**: `REQ-FUNC-011` (자동 계산), `REQ-FUNC-012` (미리보기)
* **관련 엔티티**: `PayrollRecord`, `Employee`, `Schedule`

## 3. 주요 기능 및 처리 단계
1. **계산 로직**:
   * **기본급**: 근무시간 x 시급.
   * **주휴수당**: 주 15시간 이상 만근 시 (1일분 급여).
   * **연장수당**: 일 8시간 또는 주 40시간 초과분 x 1.5 (5인 미만 사업장 예외 처리 등 옵션 확인, MVP는 표준 근로기준법 준용하되 설정 가능하게).
   * **야간수당**: 22:00 ~ 06:00 근무 시 x 0.5 추가.
2. **계산 시점**: 스케줄 승인 시점 또는 사용자가 "급여 미리보기" 조회 시점(On-demand).
3. **조회 API**: 주차별/직원별 상세 급여 명세 반환.

## 4. API 명세 (개요)
* `GET /api/v1/reports/payroll`: 급여 리포트 조회.

---

```yaml
task_id: "REQ-FUNC-011-012"
title: "급여 계산 엔진 및 미리보기 API 구현"
summary: >
  스케줄과 직원 시급 정보를 바탕으로 각종 수당(주휴, 연장, 야간)을 포함한
  급여를 자동 계산하는 엔진과 조회 API를 구현한다.
type: "functional"

epic: "E4_PAYROLL"
req_ids: ["REQ-FUNC-011", "REQ-FUNC-012"]
component: ["backend.engine", "backend.api"]

context:
  srs_section: "4.1 Functional Requirements"
  entities: ["PayrollRecord"]

inputs:
  description: >
    승인된 스케줄, 직원 계약 정보(시급).
  fields:
    - name: "schedule"
      type: "object"
    - name: "employee_wage"
      type: "decimal"

outputs:
  description: >
    직원별 급여 상세 내역.
  success:
    http_status: 200
    body: "[{ employeeId: ..., basePay: 10000, overtimePay: 5000, ... }]"

steps_hint:
  - "PayrollCalculator 클래스 구현 (Strategy Pattern 권장)."
  - "야간/연장 시간 추출 로직 작성 (TimeRange Overlap 계산)."
  - "주휴수당 계산 로직 작성 (주간 총 근무시간 기반)."
  - "계산 결과를 PayrollRecord(또는 DTO)로 변환하여 반환."

preconditions:
  - "Schedule 데이터가 있어야 함."

postconditions:
  - "정확한 수당 계산 결과가 반환되어야 함."

tests:
  unit:
    - "야간 근무 시간 계산 테스트"
    - "주휴수당 발생/미발생 조건 테스트"
  integration:
    - "스케줄 승인 후 급여 조회 시 정상 데이터 반환 확인"

dependencies: ["REQ-FUNC-008-016"]

parallelizable: true
estimated_effort: "L"
priority: "Must"
agent_profile: ["backend"]

required_tools:
  languages: ["Java"]
  frameworks: ["Spring Boot 3.x"]

references:
  srs: ["REQ-FUNC-011", "REQ-FUNC-012"]

risk_notes:
  - "5인 미만 사업장 여부에 따라 가산수당 적용 여부가 달라짐. MVP에서는 Config로 분리하거나 모두 적용하되 옵션 처리."

example_commands:
  - "REQ-FUNC-011-012 Task에 따라 급여 계산 엔진을 구현해줘."
```

