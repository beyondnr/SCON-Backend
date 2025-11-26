# AI Agent Task 정의서 활용 가이드

## 1. 개요
본 문서는 `Task_Gem3` 폴더 내에 정의된 **AI 에이전틱 프로그래밍용 Task 정의서**의 구조와 활용 방법을 설명한다. 이 정의서는 SRS(요구사항 명세서)를 기반으로 에이전트가 수행해야 할 작업을 **기계가 읽을 수 있는(Machine-readable) YAML**과 **사람이 읽을 수 있는(Human-readable) Markdown**으로 정형화한 것이다.

## 2. Task 정의서 구조

### 2.1 파일 위치
* `tasks/functional/`: 기능 요구사항(REQ-FUNC) 기반의 구현 Task들.
* `tasks/non-functional/`: 비기능 요구사항(REQ-NF) 기반의 성능/보안/운영 Task들.

### 2.2 문서 포맷
각 Task 파일(`*.md`)은 크게 두 부분으로 나뉜다.

1.  **Human-readable Section (상단)**
    *   제목, 목적, 요약, 관련 REQ, 구현 가이드 등 사람이 읽고 문맥을 파악하거나 에이전트에게 프롬프트로 제공할 자연어 설명.
2.  **Machine-readable YAML Block (하단)**
    *   `task_id`, `req_ids`, `inputs`, `outputs`, `dependencies` 등 에이전트 오케스트레이터가 파싱하여 실행 계획을 수립하는 데 사용하는 구조화된 데이터.

## 3. 활용 시나리오

### 3.1 단일 Task 실행 (Single Task Execution)
개발자가 특정 기능을 구현하고자 할 때, 해당 Task 파일을 에이전트에게 제공한다.

**예시 프롬프트:**
> "REQ-FUNC-001-003.md 파일에 정의된 내용을 바탕으로 백엔드 API 코드를 작성해줘. Spring Boot와 JPA를 사용해야 해."

### 3.2 Epic 단위 실행 (Epic Execution)
Epic 0(FE PoC)나 Epic 1(Onboarding) 등 특정 Epic에 속한 Task들을 묶어서 순차적 또는 병렬로 실행한다.

1.  `epic` 필드를 기준으로 Task 필터링.
2.  `dependencies` 필드를 확인하여 실행 순서 결정 (DAG 구성).
3.  `parallelizable: true`인 Task들은 동시에 여러 에이전트 세션에서 실행 가능.

### 3.3 의존성 및 워크플로우 관리
*   **DAG(Directed Acyclic Graph) 해석**:
    *   `A` Task의 `dependencies`에 `B`가 있다면, `B`가 완료(Completed)된 후에 `A`를 착수해야 한다.
    *   예: `REQ-FUNC-006-007`(스케줄 관리)는 `REQ-FUNC-001-003`(매장/직원 생성)이 선행되어야 테스트가 가능하다.

### 3.4 결과 검증
*   각 Task의 `outputs` 및 `tests` 항목을 기준으로 구현 결과를 검증한다.
*   `failure_cases`에 정의된 에러 상황(예: 중복 이메일 가입 시 409)이 제대로 처리되었는지 확인한다.

## 4. 메타데이터 필드 설명

| 필드명 | 설명 |
|---|---|
| `task_id` | Task 고유 식별자 (파일명과 동일 권장) |
| `req_ids` | 관련된 SRS 요구사항 ID 목록 |
| `epic` | 상위 그룹(Epic) 식별자 |
| `type` | `functional` 또는 `non_functional` |
| `inputs` / `outputs` | 입출력 데이터 명세 (API 스펙 설계 시 중요) |
| `steps_hint` | 구현 절차에 대한 핵심 힌트 (CoT 유도용) |
| `agent_profile` | 이 작업을 수행하기 적합한 에이전트 역할 (backend, frontend, infra 등) |
| `estimated_effort` | 예상 작업 크기 (S/M/L) |
| `priority` | 우선순위 (Must/Should/Could) |

## 5. 유지보수 전략
*   **SRS 변경 시**: 관련 `req_ids`를 가진 Task 파일을 찾아 내용을 갱신한다.
*   **Task 완료 시**: 완료된 Task는 상태를 업데이트하거나 결과물(코드 경로)을 메타데이터에 추가하여 추적한다.

---
*Last Updated: 2025-11-19*

