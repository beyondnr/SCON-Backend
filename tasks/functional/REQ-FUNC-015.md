# REQ-FUNC-015: 감사 로그(Audit Log) 구현

## 1. 목적 및 요약
* **목적**: 시스템 내 주요 데이터(스케줄, 급여, 규칙)의 변경 이력을 기록하여 투명성을 확보하고 분쟁 시 근거로 활용한다.
* **요약**: AOP 또는 Entity Listener를 사용하여 주요 엔터티의 CUD(Create, Update, Delete) 이벤트를 캡처하고 `AuditLog` 테이블에 저장한다.

## 2. 관련 REQ 및 엔티티
* **관련 REQ**: `REQ-FUNC-015` (감사 로그)
* **관련 엔티티**: `AuditLog`

## 3. 주요 기능 및 처리 단계
1. **로그 엔터티 정의**: `who`(Actor), `when`(Timestamp), `what`(ResourceType, ResourceId), `how`(Action), `diff`(Before/After JSON).
2. **이벤트 리스너 구현**: JPA EntityListener(`@PostPersist`, `@PostUpdate`, `@PostRemove`) 또는 Spring AOP.
3. **Diff 계산**: 변경 전 데이터와 후 데이터를 비교하여 차분 저장(선택 사항) 또는 전체 스냅샷 저장.
4. **비동기 저장**: 비즈니스 트랜잭션 성능에 영향을 덜 주도록 `@Async` 등을 활용하여 로그 저장.

---

```yaml
task_id: "REQ-FUNC-015"
title: "감사 로그(Audit Log) 시스템 구현"
summary: >
  주요 엔터티의 생성/수정/삭제 이력을 자동으로 기록하는
  Audit Logging 매커니즘을 구현한다.
type: "functional"

epic: "E6_AUDIT"
req_ids: ["REQ-FUNC-015"]
component: ["backend.core"]

context:
  srs_section: "4.1 Functional Requirements"
  entities: ["AuditLog"]

inputs:
  description: >
    내부 엔터티 변경 이벤트.
  fields:
    - name: "entity_change_event"
      type: "object"

outputs:
  description: >
    DB에 저장된 로그 레코드.
  success:
    log_created: true

steps_hint:
  - "AuditLog 엔터티 설계 (JSON 타입 컬럼 활용)."
  - "Spring Data JPA Auditing 또는 Custom EntityListener 구현."
  - "SecurityContext에서 현재 로그인한 사용자(Actor) 정보 추출 로직."
  - "Object Mapper를 사용해 Entity 상태를 JSON으로 직렬화하여 저장."

preconditions:
  - "User/Auth 모듈이 있어야 Actor 식별 가능."

postconditions:
  - "데이터 변경 시 자동으로 AuditLog 테이블에 레코드가 쌓여야 함."

tests:
  unit:
    - "EntityListener 동작 테스트"
  integration:
    - "스케줄 수정 시 Audit Log 생성 여부 확인"

dependencies: ["REQ-FUNC-001-003"]

parallelizable: true
estimated_effort: "S"
priority: "Should"
agent_profile: ["backend"]

required_tools:
  languages: ["Java"]
  frameworks: ["Spring Boot 3.x", "JPA"]

references:
  srs: ["REQ-FUNC-015"]

risk_notes:
  - "로그 데이터가 급격히 늘어날 수 있으므로 파티셔닝이나 아카이빙 정책 고려(Post-MVP)."

example_commands:
  - "REQ-FUNC-015 Task에 따라 감사 로그 기능을 구현해줘."
```

