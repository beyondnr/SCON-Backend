# REQ-FUNC-009~010: 노동법 규칙 엔진 구현 (Core)

## 1. 목적 및 요약
* **목적**: 스케줄 승인 전 노동법 위반 여부를 판단하는 핵심 규칙 엔진 로직을 구현한다.
* **요약**: `ComplianceEngine` 인터페이스와 주요 규칙(주 52시간, 휴게시간 등) 구현체를 작성하고, 스케줄 객체를 입력받아 위반 목록을 반환하는 기능을 개발한다.

## 2. 관련 REQ 및 엔티티
* **관련 REQ**: `REQ-FUNC-009` (규칙 엔진 평가), `REQ-FUNC-010` (위반 차단/가이드)
* **관련 엔티티**: `ComplianceRule` (DB 파라미터), `Schedule`

## 3. 주요 기능 및 처리 단계
1. **규칙 인터페이스 정의**: `evaluate(Schedule schedule): List<Violation>`
2. **핵심 규칙 구현**:
   * **주 52시간 제한**: 직원별 주간 총 근무시간 계산 <= 52.
   * **휴게시간 준수**: 4시간 근무 시 30분, 8시간 시 1시간 휴식(Shift 간 간격 또는 Shift 내 명시). MVP에서는 "연속 근무 4시간 초과 시 경고" 등으로 단순화 가능.
   * **주휴수당 요건 체크**: (급여 계산용이지만 스케줄 단계에서 힌트 제공 가능).
3. **위반 객체 정의**: 위반된 규칙 ID, 심각도(Block/Warning), 가이드 메시지 포함.

## 4. 구현 방식
* DB에 저장된 `ComplianceRule`의 파라미터(값)를 로드하여 로직에 적용.
* MVP는 복잡한 DSL(Drools 등) 대신 순수 Java 코드로 규칙 로직(Strategy Pattern) 구현.

---

```yaml
task_id: "REQ-FUNC-009-010"
title: "노동법 규칙 엔진 Core 구현"
summary: >
  스케줄 데이터를 분석하여 주 52시간, 휴게시간 등 노동법 위반 여부를
  판단하고 위반 목록을 반환하는 엔진 로직을 구현한다.
type: "functional"

epic: "E5_RULES"
req_ids: ["REQ-FUNC-009", "REQ-FUNC-010"]
component: ["backend.engine"]

context:
  srs_section: "4.1 Functional Requirements"
  entities: ["ComplianceRule"]

inputs:
  description: >
    검사 대상 스케줄(직원별 근무 시간 정보 포함).
  fields:
    - name: "schedule"
      type: "object"

outputs:
  description: >
    위반 사항 목록.
  success:
    return_type: "List<Violation>"
    example: "[{ruleId: 'LAW-52H', message: 'A직원 53시간 근무', severity: 'BLOCK'}]"

steps_hint:
  - "ComplianceRuleValidator 인터페이스 정의."
  - "MaxWorkHoursValidator (52시간) 구현."
  - "BreakTimeValidator (휴게시간) 구현."
  - "ComplianceEngineService에서 등록된 Validator들을 순회하며 실행."
  - "위반 사항이 있으면 승인 프로세스에서 Exception 또는 반환값으로 처리."

preconditions:
  - "Schedule 데이터 구조가 확정되어야 함."

postconditions:
  - "법규 위반 스케줄에 대해 정확한 위반 코드와 메시지가 반환된다."

tests:
  unit:
    - "52시간 초과/미만 케이스 단위 테스트"
    - "휴게시간 위반 케이스 단위 테스트"
  integration:
    - "스케줄 승인 시 규칙 엔진 호출 통합 테스트"

dependencies: ["REQ-FUNC-006-007"]

parallelizable: true
estimated_effort: "M"
priority: "Must"
agent_profile: ["backend"]

required_tools:
  languages: ["Java"]
  frameworks: ["Spring Boot 3.x"]

references:
  srs: ["REQ-FUNC-009", "REQ-FUNC-010"]

risk_notes:
  - "법규 해석의 모호함이 있을 수 있으므로, 규칙 로직을 최대한 보수적으로(안전하게) 작성."
  - "나중에 규칙이 변경되기 쉬우므로 확장 가능한 구조(Strategy) 사용."

example_commands:
  - "REQ-FUNC-009-010 Task에 따라 노동법 규칙 엔진을 구현해줘."
```

