# REQ-NF-PERF: 성능 목표 달성 및 검증

## 1. 목적 및 요약
* **목적**: 주요 사용자 시나리오(스케줄 조회, 승인, 문서 생성)에서 목표 응답 시간(Latency)을 달성하도록 시스템을 튜닝하고 검증한다.
* **요약**: p95 기준 0.8초~5초 이내의 응답 시간을 보장하기 위해 인덱스 최적화, 쿼리 튜닝, 비동기 처리, 그리고 부하 테스트를 수행한다.

## 2. 관련 REQ
* `REQ-NF-001`: 스케줄 보드 조회 (p95 ≤ 0.8s)
* `REQ-NF-002`: 승인 처리 (p95 ≤ 1.2s)
* `REQ-NF-003`: 증빙 생성 (≤ 5s)

## 3. 요구사항 상세
* **조회 성능**: `GET /schedules` 호출 시 복잡한 조인(Shift, Employee)이 있어도 0.8초 이내 응답.
* **트랜잭션 성능**: 승인 버튼 클릭 시 규칙 엔진 계산과 상태 업데이트가 1.2초 이내 완료.
* **처리량**: 문서 생성 등 무거운 작업은 5초 이내 완료하거나 비동기 처리 후 폴링(MVP는 동기 5초 목표).

## 4. 구현 및 검증 활동
1. **DB 인덱스 설계**: `store_id`, `week_of`, `employee_id` 등 조회 조건 컬럼 인덱싱.
2. **N+1 문제 해결**: JPA `Fetch Join` 또는 `EntityGraph` 사용하여 쿼리 수 최소화.
3. **부하 테스트**: k6 또는 JMeter를 사용하여 동시 접속자(가상) 부하 상황 시뮬레이션.

---

```yaml
task_id: "REQ-NF-PERF"
title: "핵심 기능 성능 최적화 및 부하 테스트"
summary: >
  스케줄 조회/승인 및 증빙 생성 기능에 대해
  SRS에 정의된 목표 응답 시간(p95)을 달성하고 검증한다.
type: "non_functional"

epic: "E9_NFR"
req_ids: ["REQ-NF-001", "REQ-NF-002", "REQ-NF-003"]
component: ["backend.api", "database"]

category: "performance"
labels:
  - "performance:latency"
  - "performance:db_tuning"

context:
  srs_section: "4.2 Non-Functional Requirements"
  related_req_func: ["REQ-FUNC-006", "REQ-FUNC-008", "REQ-FUNC-013"]

requirements:
  description: >
    사용자 체감 성능을 보장하기 위한 Latency 임계값 준수.
  kpis:
    - "GET /schedules p95 Latency <= 800ms"
    - "POST /approve p95 Latency <= 1200ms"
    - "Evidence Generation <= 5000ms"

implementation_scope:
  includes:
    - "DB 쿼리 최적화 (Index, Fetch Join)"
    - "비즈니스 로직 경량화"
    - "부하 테스트 스크립트 작성 (k6/JMeter)"

steps_hint:
  - "Hibernate SQL 로그 확인하여 불필요한 쿼리 제거."
  - "실행 계획(Explain) 분석하여 인덱스 적용."
  - "문서 생성 시 스트리밍 방식 등 메모리 효율적 방법 적용."
  - "CI 파이프라인에 성능 테스트 단계 추가(선택)."

preconditions:
  - "기능 구현이 완료되고 테스트 데이터가 확보되어야 함."

postconditions:
  - "부하 테스트 리포트에서 모든 KPI가 Pass되어야 함."

tests:
  - "가상 유저 50명 동시 접속 시나리오 테스트"

dependencies: ["REQ-FUNC-006-007", "REQ-FUNC-008-016", "REQ-FUNC-013-014"]

parallelizable: true
estimated_effort: "M"
priority: "Must"
agent_profile: ["backend", "qa"]

required_tools:
  languages: ["JavaScript(k6)"]
  infra: ["MySQL"]

references:
  srs: ["REQ-NF-001", "REQ-NF-002", "REQ-NF-003"]

risk_notes:
  - "초기 데이터가 적을 땐 성능 이슈가 안 보일 수 있으므로 더미 데이터 충분히 생성 후 테스트."

```

