# REQ-FUNC-006~007: 스케줄 관리 및 편집 API 구현

## 1. 목적 및 요약
* **목적**: 사장님이 대시보드에서 스케줄을 조회하고, 수정하고(Draft), 관리할 수 있는 백엔드 기능을 구현한다.
* **요약**: `Schedule` 엔터티와 주차별/매장별 조회 API, 그리고 스케줄 수정(Update Draft) API를 개발한다.

## 2. 관련 REQ 및 엔티티
* **관련 REQ**: `REQ-FUNC-006` (대시보드 조회), `REQ-FUNC-007` (스케줄 편집)
* **관련 엔티티**: `Schedule`, `Shift`

## 3. 주요 기능 및 처리 단계
1. **스케줄 생성/조회**: 특정 주차(Week)에 스케줄이 없으면 `AvailabilitySubmission`을 기반으로 초기 Draft를 생성하거나 빈 스케줄을 반환.
2. **스케줄 상세 조회**: 스케줄에 포함된 모든 Shift 정보와 직원 정보, 승인 상태(`status`)를 함께 반환.
3. **스케줄 수정**:
   * 특정 Shift의 시간 변경, 삭제, 추가.
   * 수정 시 상태는 항상 `DRAFT`로 유지(승인된 스케줄 수정 시 새 버전 생성 또는 Draft로 전환).
   * 변경 사항 저장 시 `updatedAt` 갱신.

## 4. API 명세 (개요)
* `GET /api/v1/schedules`: 매장/주차별 스케줄 조회 (없으면 생성 후 반환 가능).
* `PUT /api/v1/schedules/{scheduleId}`: 스케줄 내용(Shift 목록) 수정.

---

```yaml
task_id: "REQ-FUNC-006-007"
title: "스케줄 관리 및 편집 API 구현"
summary: >
  스케줄(Shift) 조회, 생성, 수정을 처리하는 백엔드 API를 구현한다.
  Draft 상태 관리와 버전 제어를 포함한다.
type: "functional"

epic: "E3_SCHEDULE"
req_ids: ["REQ-FUNC-006", "REQ-FUNC-007"]
component: ["backend.api"]

context:
  srs_section: "4.1 Functional Requirements"
  entities: ["Schedule", "Shift"]

inputs:
  description: >
    스케줄 ID 및 수정할 Shift 리스트.
  fields:
    - name: "shifts"
      type: "array"
      description: "수정된 교대 정보 목록"

outputs:
  description: >
    갱신된 스케줄 객체.
  success:
    http_status: 200
    body: "{ id: ..., status: 'DRAFT', shifts: [...] }"

steps_hint:
  - "Schedule - Shift(1:N or JSON) 엔터티 매핑."
  - "GET 요청 시 가용시간 데이터를 병합하여 보여줄지, 별도 API로 할지 결정 (여기선 Schedule 중심)."
  - "PUT 요청 처리 시 기존 Shift를 덮어쓰거나 Diff 업데이트."
  - "Optimistic Locking(@Version) 적용 고려 (동시 수정 방지)."

preconditions:
  - "Store, Employee 데이터가 존재해야 함."

postconditions:
  - "스케줄 수정 사항이 DB에 반영되고 상태는 Draft가 된다."

tests:
  unit:
    - "스케줄 업데이트 로직 테스트"
  integration:
    - "스케줄 생성 -> 조회 -> 수정 흐름 테스트"

dependencies: ["REQ-FUNC-001-003"]

parallelizable: true
estimated_effort: "M"
priority: "Must"
agent_profile: ["backend"]

required_tools:
  languages: ["Java"]
  frameworks: ["Spring Boot 3.x"]

references:
  srs: ["REQ-FUNC-006", "REQ-FUNC-007"]

risk_notes:
  - "Shift 데이터가 많을 경우 JSON 타입 저장 vs 별도 테이블 정규화 성능/관리 포인트 고려 (MVP는 JSON 권장)."

example_commands:
  - "REQ-FUNC-006-007 Task에 따라 스케줄 조회/수정 API를 구현해줘."
```

