# REQ-FUNC-008~016: 스케줄 승인 워크플로우 및 알림 구현

## 1. 목적 및 요약
* **목적**: 사장님이 스케줄을 최종 승인(Approve)하는 액션을 처리하고, 승인 시 자동으로 규칙 엔진을 실행하며, 성공 시 직원에게 알림을 발송한다.
* **요약**: 승인 API, 상태 변경 로직, 규칙 엔진 연동, 알림 서비스 호출을 포함한 오케스트레이션 로직을 구현한다.

## 2. 관련 REQ 및 엔티티
* **관련 REQ**: `REQ-FUNC-008` (승인), `REQ-FUNC-016` (알림)
* **관련 엔티티**: `Schedule`, `NotificationLog`

## 3. 주요 기능 및 처리 단계
1. **승인 요청 수신**: `POST /schedules/{id}/approve`.
2. **규칙 엔진 실행**: `ComplianceEngine` 호출. 위반 발생 시 409 에러 반환(승인 중단).
3. **상태 변경**: 위반 없으면 Schedule Status를 `APPROVED`로 변경, `approvedAt` 기록.
4. **후처리 (비동기/동기)**:
   * 급여 계산 트리거 (Event 발행).
   * 직원 알림 발송 (NotificationService 호출).
5. **알림 발송**: 카카오톡/SMS 게이트웨이(Mock)를 통해 해당 스케줄에 포함된 직원들에게 메시지 전송.

## 4. API 명세 (개요)
* `POST /api/v1/schedules/{id}/approve`: 승인 처리.

---

```yaml
task_id: "REQ-FUNC-008-016"
title: "스케줄 승인 워크플로우 및 알림 구현"
summary: >
  스케줄 승인 시 규칙 검사, 상태 변경, 직원 알림 발송을 수행하는
  비즈니스 로직을 구현한다.
type: "functional"

epic: "E3_SCHEDULE"
req_ids: ["REQ-FUNC-008", "REQ-FUNC-016"]
component: ["backend.api", "notification-service"]

context:
  srs_section: "4.1 Functional Requirements"
  entities: ["Schedule"]

inputs:
  description: >
    승인 대상 스케줄 ID.
  fields:
    - name: "scheduleId"
      type: "uuid"

outputs:
  description: >
    승인 결과 및 알림 발송 카운트.
  success:
    http_status: 200
    body: "{ status: 'APPROVED', notifiedCount: 5 }"
  failure_cases:
    - code: "RULE_VIOLATION"
      http_status: 409
      description: "규칙 위반으로 승인 불가"

steps_hint:
  - "ApproveService 메서드 구현."
  - "Transaction 내에서 규칙 검사 -> 상태 변경 수행."
  - "NotificationService 인터페이스 및 Mock 구현체 작성."
  - "Observer 패턴 또는 Spring Event를 사용하여 승인 후 알림/급여계산 로직 분리."

preconditions:
  - "규칙 엔진(REQ-FUNC-009-010)이 구현되어 있어야 함."

postconditions:
  - "승인된 스케줄은 수정이 불가능해야 함(또는 수정 시 새 버전)."
  - "직원들에게 알림이 발송되었다는 로그가 남아야 함."

tests:
  unit:
    - "규칙 위반 시 승인 롤백 테스트"
    - "승인 성공 시 상태 변경 테스트"
  integration:
    - "승인 API 호출 -> 알림 Mock 호출 확인"

dependencies: ["REQ-FUNC-009-010"]

parallelizable: false
estimated_effort: "M"
priority: "Must"
agent_profile: ["backend"]

required_tools:
  languages: ["Java"]
  frameworks: ["Spring Boot 3.x"]

references:
  srs: ["REQ-FUNC-008", "REQ-FUNC-016"]

risk_notes:
  - "알림 발송 실패가 승인 트랜잭션을 롤백시키면 안 됨(알림은 Best Effort 또는 재시도)."

example_commands:
  - "REQ-FUNC-008-016 Task에 따라 스케줄 승인 및 알림 로직을 구현해줘."
```

