# EPIC0-FE-003: 사장님 스케줄 대시보드 및 승인 PoC UI 구현

## 1. 목적 및 요약
* **목적**: 사장님이 직원들이 제출한 가용시간을 바탕으로 생성된 스케줄을 검토하고, 수정하고, 최종 승인하는 핵심 대시보드 화면을 프로토타이핑한다.
* **요약**: 스케줄 캘린더/테이블 뷰, 승인 대기 상태 카드, 규칙 위반 알림 표시, 그리고 1-Click 승인 버튼 동작을 구현한다.

## 2. 관련 SRS 및 요구사항
* **관련 REQ**: `REQ-FUNC-006` (승인 대기), `REQ-FUNC-007` (스케줄 편집), `REQ-FUNC-008` (승인), `REQ-FUNC-010` (위반 가이드)
* **관련 Epic**: Epic 0 (FE Prototyping)

## 3. PoC 범위 및 비범위
* **In-Scope**:
  * 주간 스케줄을 보여주는 테이블/캘린더 뷰 (직원 x 요일 매트릭스).
  * "승인 대기" 상태를 알리는 상단 대시보드 카드 UI.
  * 특정 교대(Shift) 클릭 시 시간 수정 모달/팝오버.
  * "승인" 버튼 클릭 시 규칙 위반 시뮬레이션(빨간색 경고 표시) 및 성공 시뮬레이션.
* **Out-of-Scope**:
  * 정교한 Drag & Drop (DnD) 라이브러리 통합 (PoC는 클릭->수정 방식 우선).
  * 실제 규칙 엔진 연동 (위반 상황은 하드코딩된 Mock 데이터로 재현).

## 4. 주요 화면 구성
1. **Dashboard Main**: 상단 요약 카드(승인 대기 건수, 금주 예상 인건비).
2. **Schedule View**: 가로축(날짜/요일), 세로축(직원) 형태의 그리드.
   * 셀 내부에 근무 시간 표시.
3. **Action Bar**: "스케줄 승인", "직원에게 알림 발송" 버튼 그룹.
4. **Validation Feedback**: 승인 시도 시 "주 52시간 초과" 등의 에러 메시지 토스트/모달.

## 5. 구현 가이드 (Steps Hint)
1. `pages/Dashboard` 라우트 생성.
2. Grid Layout을 사용하여 주간 스케줄 테이블 구현.
3. Mock Data(직원 목록, 이번 주 Shift 목록)를 정의하여 렌더링.
4. Shift 셀 클릭 시 시간을 수정할 수 있는 Popover 구현.
5. 승인 버튼 핸들러에 Random Success/Fail 로직을 넣어, 위반 시 에러 UI가 어떻게 뜨는지 구현.

---

```yaml
task_id: "EPIC0-FE-003"
title: "사장님 스케줄 대시보드 및 승인 PoC UI 구현"
summary: >
  사장님이 스케줄을 조회/수정하고 규칙 위반 여부를 확인한 뒤
  최종 승인하는 핵심 대시보드 UI를 프로토타이핑한다.
type: "functional"

epic: "EPIC_0_FE_PROTOTYPE"
req_ids: ["REQ-FUNC-006", "REQ-FUNC-007", "REQ-FUNC-008", "REQ-FUNC-010"]
component: ["frontend.ui"]

context:
  srs_section: "4.1 Functional Requirements"
  user_personas: ["ST-01 사장님"]

inputs:
  description: >
    Mock 스케줄 데이터 및 사용자 편집 액션.
  fields:
    - name: "weekly_schedule"
      type: "array"
      description: "Shift 객체 리스트"

outputs:
  description: >
    수정된 스케줄 데이터 및 승인 상태 변경.
  success:
    ui_feedback: "승인 완료 뱃지 표시 및 알림 전송 토스트"

steps_hint:
  - "CSS Grid를 활용해 반응형 스케줄 테이블 작성."
  - "가용시간 미제출자/제출자 시각적 구분."
  - "승인 버튼 클릭 -> Mock 검증 -> 성공/실패 분기 UI 처리."
  - "위반 메시지는 직관적으로(빨간색, 경고 아이콘) 표현."

preconditions:
  - "기본 레이아웃 컴포넌트(헤더, 사이드바 등)가 준비되어 있다고 가정."

postconditions:
  - "스케줄을 눈으로 확인하고 버튼 하나로 승인하는 경험이 전달되어야 한다."

tests:
  unit:
    - "스케줄 데이터 렌더링 로직 테스트"
  e2e:
    - "스케줄 수정 -> 승인 -> 위반 경고 확인 시나리오"

dependencies: []

parallelizable: true
estimated_effort: "M"
priority: "Must"
agent_profile: ["frontend"]

required_tools:
  languages: ["TypeScript", "TSX"]
  frameworks: ["React", "Vite", "TailwindCSS"]

references:
  srs: ["REQ-FUNC-006", "REQ-FUNC-008"]

risk_notes:
  - "스케줄 그리드 UI 구현 난이도가 높을 수 있음 (CSS Grid 활용 권장)."

example_commands:
  - "EPIC0-FE-003 Task에 따라 스케줄 대시보드 UI를 구현해줘."
```

