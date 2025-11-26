# EPIC0-FE-002: 직원 가용시간 제출 모바일 폼 PoC UI 구현

## 1. 목적 및 요약
* **목적**: 직원들이 별도 로그인 없이 링크를 통해 자신의 근무 가능 시간을 제출하는 모바일 웹 페이지의 프로토타입을 구현한다.
* **요약**: 모바일 환경에 최적화된 가용시간 선택 UI(시간표 그리드 또는 리스트 선택 방식)를 구현하고, 제출 시 간단한 유효성 검증 피드백을 제공한다.

## 2. 관련 SRS 및 요구사항
* **관련 REQ**: `REQ-FUNC-004` (가용시간 제출), `REQ-FUNC-005` (유효성 검증 - UI단)
* **관련 Epic**: Epic 0 (FE Prototyping)

## 3. PoC 범위 및 비범위
* **In-Scope**:
  * 모바일 뷰포트에 최적화된 반응형 UI.
  * 주차(Week) 정보 표시 및 요일별 시간 선택 인터페이스.
  * "제출하기" 버튼 클릭 시 선택된 시간 데이터 구조화.
  * 기본적인 입력 오류(시간 누락 등) 알림.
* **Out-of-Scope**:
  * 실제 토큰 검증 및 백엔드 전송.
  * 복잡한 드래그 앤 드롭(터치) 인터랙션 (MVP는 Select Box나 간단한 터치 토글로 시작).

## 4. 주요 화면 구성
1. **Intro**: "OOO 매장 X월 X주차 근무 가능 시간을 입력해주세요" 안내 메시지.
2. **Time Picker**: 요일별로 근무 가능 시작/종료 시간을 추가하는 UI. (예: "월요일 09:00 ~ 14:00" + 버튼)
3. **Confirmation**: 제출 전 입력 내용 요약 및 "제출" 버튼.
4. **Success**: "제출되었습니다" 완료 화면.

## 5. 구현 가이드 (Steps Hint)
1. `pages/AvailabilityForm` 라우트를 생성한다.
2. 모바일 First로 CSS(Tailwind 등)를 작성한다.
3. 요일(Mon-Sun) 탭 또는 스크롤 뷰를 구성한다.
4. 시간 선택 컴포넌트(Native Time Input 또는 Custom Picker)를 배치한다.
5. 제출 버튼 클릭 시 JSON 데이터를 콘솔에 출력하고 완료 화면으로 전환한다.

---

```yaml
task_id: "EPIC0-FE-002"
title: "직원 가용시간 제출 모바일 폼 PoC UI 구현"
summary: >
  직원이 모바일에서 근무 가능 시간을 손쉽게 입력하고 제출할 수 있는
  반응형 웹 폼 UI를 프로토타이핑한다.
type: "functional"

epic: "EPIC_0_FE_PROTOTYPE"
req_ids: ["REQ-FUNC-004", "REQ-FUNC-005"]
component: ["frontend.mobile"]

context:
  srs_section: "4.1 Functional Requirements"
  user_personas: ["ST-02 직원"]

inputs:
  description: >
    직원의 요일별 근무 가능 시간 범위 선택.
  fields:
    - name: "availability_list"
      type: "array"
      description: "[{day, start, end}, ...]"

outputs:
  description: >
    제출된 가용시간 데이터 리스트.
  success:
    ui_feedback: "완료 메시지 및 확인 아이콘 표시"

steps_hint:
  - "모바일 해상도(375px~) 기준으로 레이아웃 잡기."
  - "요일별 탭 또는 아코디언 UI 적용."
  - "시간 입력 시 시작 시간이 종료 시간보다 늦지 않도록 검증."
  - "Mock API 호출(setTimeout) 후 성공 화면 전환."

preconditions:
  - "공통 컴포넌트(버튼, 인풋 등)가 존재하면 재사용한다."

postconditions:
  - "모바일 화면에서 깨짐 없이 시간 입력 및 제출 동작이 시뮬레이션되어야 한다."

tests:
  unit:
    - "시간 범위 유효성 로직(Start < End) 테스트"
  e2e:
    - "모바일 뷰포트에서 입력 -> 제출 플로우 테스트"

dependencies: []

parallelizable: true
estimated_effort: "S"
priority: "Must"
agent_profile: ["frontend"]

required_tools:
  languages: ["TypeScript", "TSX"]
  frameworks: ["React", "Vite", "TailwindCSS"]

references:
  srs: ["REQ-FUNC-004"]

risk_notes:
  - "모바일 브라우저별 Time Picker UI 차이 고려."

example_commands:
  - "EPIC0-FE-002 Task에 따라 모바일 가용시간 제출 폼을 구현해줘."
```

