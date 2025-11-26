# EPIC0-FE-001: 온보딩 및 매장/직원 관리 PoC UI 구현

## 1. 목적 및 요약
* **목적**: 서비스의 첫인상을 결정하는 온보딩 플로우와 기초 데이터(매장, 직원) 관리 화면의 프론트엔드 프로토타입을 구현한다.
* **요약**: 사장님 회원가입, 매장 정보 입력, 직원 등록으로 이어지는 3단계 마법사 UI와, 이후 대시보드 내에서 매장/직원 정보를 조회하고 수정하는 관리 페이지를 구현한다.

## 2. 관련 SRS 및 요구사항
* **관련 REQ**: `REQ-FUNC-001` (3단계 온보딩), `REQ-FUNC-002` (매장 정보), `REQ-FUNC-003` (직원 등록)
* **관련 Epic**: Epic 0 (FE Prototyping)

## 3. PoC 범위 및 비범위
* **In-Scope**:
  * 3단계 위자드 형태의 UI 구조 (Step 1: 가입 -> Step 2: 매장 -> Step 3: 직원).
  * 각 입력 폼의 레이아웃 및 기본 유효성 검사(필수값 등).
  * 입력된 데이터를 상태 관리 라이브러리(예: Zustand, Recoil 등)에 저장하고 화면 간 유지.
  * "완료" 버튼 클릭 시 콘솔에 수집된 데이터 출력.
* **Out-of-Scope**:
  * 실제 백엔드 API 연동 (Mocking으로 처리).
  * 복잡한 애니메이션 효과.
  * 실제 인증(JWT) 처리 로직.

## 4. 주요 화면 구성
1. **Landing/Login**: 심플한 랜딩 및 "시작하기" 버튼.
2. **Onboarding Wizard**:
   * Step 1: 사장님 계정 정보 (이메일, 비번).
   * Step 2: 매장 정보 (이름, 업종, 영업시간).
   * Step 3: 초기 직원 등록 (이름, 시급, 역할).
3. **Settings Page**: 온보딩 완료 후 진입 가능한 매장/직원 목록 수정 페이지.

## 5. 구현 가이드 (Steps Hint)
1. React 프로젝트 구조 안에 `pages/Onboarding`, `pages/Settings` 라우트를 생성한다.
2. 재사용 가능한 Form Component(Input, Select, Button)를 작성한다.
3. 3단계 상태를 관리하는 Wizard Context 또는 State를 구성한다.
4. 각 단계별 유효성 검사 로직을 간단히 구현한다.
5. 최종 완료 시 전체 데이터를 JSON 형태로 콘솔에 로그를 찍고, 메인 대시보드로 이동하는 척(Mock Navigation) 한다.

---

```yaml
task_id: "EPIC0-FE-001"
title: "온보딩 및 매장/직원 관리 PoC UI 구현"
summary: >
  신규 사장님을 위한 3단계 온보딩(가입-매장-직원) 위자드 UI와
  기초 설정 관리 화면을 프로토타이핑한다.
type: "functional"

epic: "EPIC_0_FE_PROTOTYPE"
req_ids: ["REQ-FUNC-001", "REQ-FUNC-002", "REQ-FUNC-003"]
component: ["frontend.ui"]

context:
  srs_section: "4.1 Functional Requirements"
  user_personas: ["ST-01 사장님"]

inputs:
  description: >
    사용자 입력(Form Input) 및 UI 상호작용.
  fields:
    - name: "owner_info"
      type: "object"
      description: "이메일, 비밀번호"
    - name: "store_info"
      type: "object"
      description: "매장명, 업종, 영업시간"
    - name: "employee_list"
      type: "array"
      description: "직원 이름, 시급, 역할 목록"

outputs:
  description: >
    온보딩 완료 시 수집된 전체 데이터 객체(JSON).
  success:
    ui_feedback: "대시보드 메인 화면으로 전환"
    console_log: "Collected Onboarding Data"

steps_hint:
  - "React Router를 이용해 /onboarding 경로 설정."
  - "Step 1, 2, 3 컴포넌트 분리 및 상태 공유(State Management)."
  - "각 단계 '다음' 버튼 클릭 시 유효성 검사(Validation)."
  - "직원 등록 단계에서는 '직원 추가' 버튼으로 동적 리스트 관리 UI 구현."

preconditions:
  - "React + Vite 기본 프로젝트가 세팅되어 있어야 한다."

postconditions:
  - "사용자는 3단계를 거쳐 정보를 입력하고, 최종적으로 입력한 정보를 확인할 수 있다."

tests:
  unit:
    - "입력 폼 Validation 동작 테스트"
  e2e:
    - "Wizard 1단계부터 3단계까지 순차 진행 플로우 테스트 (Manual/Cypress)"

dependencies: []

parallelizable: true
estimated_effort: "S"
priority: "Must"
agent_profile: ["frontend"]

required_tools:
  languages: ["TypeScript", "TSX"]
  frameworks: ["React", "Vite", "TailwindCSS"]

references:
  srs: ["REQ-FUNC-001"]

risk_notes:
  - "입력 필드가 많아 UI가 복잡해질 수 있으므로 모바일 반응형 고려 필요."

example_commands:
  - "EPIC0-FE-001 Task에 따라 온보딩 위자드 UI를 구현해줘."
```

