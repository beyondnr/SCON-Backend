---
title: "[REQ-FUNC-011~012] 급여 계산 엔진 구현"
labels: ["feature", "core", "payroll"]
assignees: []
---

## 1. 목적
승인된 스케줄을 바탕으로 각종 수당을 포함한 예상 급여를 자동 계산하는 엔진을 구현한다.

## 2. 범위
- `REQ-FUNC-011`: 자동 계산 (기본급, 연장, 야간, 휴일수당).
- `REQ-FUNC-012`: 급여 미리보기 API.

## 3. 상세 작업
- [ ] `PayrollCalculator` 로직 구현.
- [ ] 연장근로 계산 로직 구현 (일 8시간 / 주 40시간 초과).
- [ ] 야간근로 계산 로직 구현 (22:00 ~ 06:00).
- [ ] `PayrollController` 구현 (계산된 데이터 반환).

## 4. 완료 조건 (Acceptance Criteria)
- 기본급 및 각종 수당이 정확하게 계산되어야 한다.
- 특정 주차 및 매장에 대한 상세 급여 리포트가 반환되어야 한다.

## 5. 참고 자료
- `tasks/functional/REQ-FUNC-011-012.md`
