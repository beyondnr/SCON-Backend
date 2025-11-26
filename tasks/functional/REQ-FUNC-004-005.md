# REQ-FUNC-004~005: 가용시간 제출 API 및 검증 로직 구현

## 1. 목적 및 요약
* **목적**: 직원들이 자신의 근무 가능 시간을 제출하는 엔드포인트와, 제출된 데이터의 유효성(중복, 영업시간 내 포함 여부 등)을 검증하는 로직을 구현한다.
* **요약**: 인증 없이(또는 간편 인증) 접근 가능한 공개 API와 `AvailabilitySubmission` 엔터티, 그리고 중복/충돌 검사 Validator를 개발한다.

## 2. 관련 REQ 및 엔티티
* **관련 REQ**: `REQ-FUNC-004` (제출 API), `REQ-FUNC-005` (검증), `REQ-FUNC-018` (승인 차단 연계)
* **관련 엔티티**: `AvailabilitySubmission`, `Employee`

## 3. 주요 기능 및 처리 단계
1. **토큰 검증**: URL에 포함된 토큰(직원/주차 식별용) 유효성 확인.
2. **데이터 파싱**: JSON으로 넘어온 요일별 시간대(TimeRange) 파싱.
3. **유효성 검사 (Validator)**:
   * 시작시간 < 종료시간 확인.
   * 이미 제출된 시간과 겹치는지 확인.
   * 매장 영업시간 범위를 벗어나는지 확인 (Warning).
4. **저장**: 검증 통과 시 `AvailabilitySubmission` 저장.
5. **실패 처리**: 검증 실패 시 400 Bad Request 및 상세 에러 메시지(UI 표시용) 반환.

## 4. API 명세 (개요)
* `POST /api/v1/availability`: 가용시간 제출.
* `GET /api/v1/availability/{token}`: 기 제출된 내용 조회 (수정 시).

---

```yaml
task_id: "REQ-FUNC-004-005"
title: "가용시간 제출 API 및 검증 로직 구현"
summary: >
  직원 가용시간 제출을 처리하는 API와 시간 중복/유효성을 검사하는
  백엔드 로직을 구현한다.
type: "functional"

epic: "E2_AVAILABILITY"
req_ids: ["REQ-FUNC-004", "REQ-FUNC-005", "REQ-FUNC-018"]
component: ["backend.api"]

context:
  srs_section: "4.1 Functional Requirements"
  entities: ["AvailabilitySubmission"]

inputs:
  description: >
    직원 식별 토큰, 주차 정보, 요일별 시간대 목록.
  fields:
    - name: "time_ranges"
      type: "array"
      description: "[{day: 'MON', start: '09:00', end: '12:00'}, ...]"

outputs:
  description: >
    제출 성공 여부 및 검증 오류 목록.
  success:
    http_status: 200
  failure_cases:
    - code: "OVERLAPPING_TIME"
      description: "시간대가 겹칩니다."

steps_hint:
  - "AvailabilitySubmission 엔터티 모델링 (JSON 컬럼 활용 고려)."
  - "TimeRangeOverlapsValidator 구현 (Interval Tree 또는 단순 루프)."
  - "제출 시 기존 데이터가 있으면 덮어쓰기(Update) 또는 버전 관리 정책 적용."
  - "입력 오류 발생 시 상세 메시지를 반환하여 FE가 표시할 수 있게 함."

preconditions:
  - "Employee 엔터티가 구현되어 있어야 함."

postconditions:
  - "유효한 시간대만 DB에 저장된다."
  - "중복된 시간 입력 시 400 에러가 발생한다."

tests:
  unit:
    - "시간 겹침 검사 알고리즘 단위 테스트"
  integration:
    - "API를 통한 제출 및 검증 실패 케이스 테스트"

dependencies: ["REQ-FUNC-001-003"]

parallelizable: true
estimated_effort: "S"
priority: "Must"
agent_profile: ["backend"]

required_tools:
  languages: ["Java"]
  frameworks: ["Spring Boot 3.x"]

references:
  srs: ["REQ-FUNC-004", "REQ-FUNC-005"]

risk_notes:
  - "Timezone 처리 주의 (KST 기준)."

example_commands:
  - "REQ-FUNC-004-005 Task에 따라 가용시간 제출 API를 구현해줘."
```

