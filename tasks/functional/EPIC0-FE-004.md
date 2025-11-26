# EPIC0-FE-004: 급여 미리보기 및 리포트 PoC UI 구현

## 1. 목적 및 요약
* **목적**: 승인된 스케줄을 기반으로 계산된 예상 급여 명세서와 리포트를 보여주는 화면을 프로토타이핑한다.
* **요약**: 직원별 기본급, 수당(주휴/연장/야간)이 상세히 나열된 급여 테이블과 증빙 다운로드 버튼 UI를 구현한다.

## 2. 관련 SRS 및 요구사항
* **관련 REQ**: `REQ-FUNC-011` (자동 계산 - 표시), `REQ-FUNC-012` (미리보기), `REQ-FUNC-013` (증빙 생성), `REQ-FUNC-021` (인쇄 레이아웃)
* **관련 Epic**: Epic 0 (FE Prototyping)

## 3. PoC 범위 및 비범위
* **In-Scope**:
  * 직원별 급여 합계 및 상세 항목(기본급, 주휴수당 등)을 보여주는 테이블.
  * 전월/전주 대비 변동사항 시각적 표시(색상 하이라이트).
  * "증빙 다운로드" 버튼 (클릭 시 더미 파일 다운로드 또는 알림).
  * 브라우저 인쇄(`Ctrl+P`) 시 깔끔하게 나오는 CSS Print Media Query 적용.
* **Out-of-Scope**:
  * 실제 급여 계산 로직 (프론트엔드에서는 계산된 결과값만 받아 뿌려주는 역할).
  * PDF 생성 (백엔드 역할).

## 4. 주요 화면 구성
1. **Report Header**: 조회 기간(주차/월) 선택 및 총 지출 예상액.
2. **Payroll Table**:
   * Row: 직원 이름.
   * Columns: 총 근무시간, 시급, 기본급, 각종 수당, **총 지급액**.
   * Expandable Row: 클릭 시 일별 근무 기록 상세 표시.
3. **Export Controls**: "Excel 다운로드", "PDF 다운로드", "인쇄" 버튼.

## 5. 구현 가이드 (Steps Hint)
1. `pages/Reports` 라우트 생성.
2. 직원별 급여 Mock Data 구조 설계 (급여 항목 세분화).
3. 테이블 컴포넌트 구현 (Sort/Filter 기능은 PoC에서 선택 사항).
4. `@media print` CSS를 작성하여 인쇄 시 네비게이션 바 숨기기 등 처리.
5. 수당 항목(야간, 주휴 등)이 있는 경우 뱃지나 색상으로 강조.

---

```yaml
task_id: "EPIC0-FE-004"
title: "급여 미리보기 및 리포트 PoC UI 구현"
summary: >
  승인된 스케줄에 따른 예상 급여와 수당 상세 내역을 조회하고
  증빙을 다운로드할 수 있는 리포트 UI를 프로토타이핑한다.
type: "functional"

epic: "EPIC_0_FE_PROTOTYPE"
req_ids: ["REQ-FUNC-011", "REQ-FUNC-012", "REQ-FUNC-021"]
component: ["frontend.ui"]

context:
  srs_section: "4.1 Functional Requirements"
  user_personas: ["ST-01 사장님"]

inputs:
  description: >
    계산 완료된 급여 리포트 Mock Data.
  fields:
    - name: "payroll_report"
      type: "array"
      description: "직원별 급여 상세 항목 리스트"

outputs:
  description: >
    화면 렌더링 및 인쇄 화면.
  success:
    ui_feedback: "데이터 테이블 표시 및 인쇄 미리보기 정상 동작"

steps_hint:
  - "테이블 UI로 상세 급여 항목 표현."
  - "Tailwind의 print:hidden 유틸리티 등을 사용해 인쇄 스타일 적용."
  - "금액 포맷팅(3자리 콤마) 유틸리티 적용."
  - "Mock Download 버튼 클릭 이벤트 처리."

preconditions:
  - "대시보드 레이아웃 내에서 동작."

postconditions:
  - "사용자가 급여 내역을 명확히 인지하고 인쇄할 수 있어야 한다."

tests:
  unit:
    - "금액 포맷팅 함수 테스트"
  e2e:
    - "리포트 페이지 진입 -> 인쇄 버튼 클릭 시뮬레이션"

dependencies: []

parallelizable: true
estimated_effort: "S"
priority: "Should"
agent_profile: ["frontend"]

required_tools:
  languages: ["TypeScript", "TSX"]
  frameworks: ["React", "Vite", "TailwindCSS"]

references:
  srs: ["REQ-FUNC-012", "REQ-FUNC-021"]

risk_notes:
  - "항목이 많아 가로 스크롤이 발생할 수 있음. 모바일 대응 전략 필요."

example_commands:
  - "EPIC0-FE-004 Task에 따라 급여 리포트 UI를 구현해줘."
```

