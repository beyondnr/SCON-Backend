---
title: "[REQ-FUNC-009~010] 노동법 규칙 엔진 핵심 로직 구현"
labels: ["feature", "core", "compliance"]
assignees: []
---

## 1. 목적
노동법(규칙) 준수 여부를 판단하기 위해 스케줄을 평가하는 핵심 엔진을 구현한다.

## 2. 범위
- `REQ-FUNC-009`: 규칙 평가 엔진.
- `REQ-FUNC-010`: 위반 감지 및 차단(Block).

## 3. 상세 작업
- [ ] `ComplianceRule` 인터페이스 및 전략(Strategy) 패턴 정의.
- [ ] `MaxWorkHoursRule` 구현 (주 52시간 제한).
- [ ] `BreakTimeRule` 구현 (휴게시간 준수).
- [ ] `ComplianceService` 구현 (스케줄 평가 후 `Violation` 목록 반환).

## 4. 완료 조건 (Acceptance Criteria)
- 엔진이 주 52시간을 초과하는 스케줄을 정확히 감지해야 한다.
- 엔진이 휴게시간 누락을 정확히 감지해야 한다.
- 위반 사항 목록과 심각도(BLOCK/WARNING)가 반환되어야 한다.

## 5. 참고 자료
- `tasks/functional/REQ-FUNC-009-010.md`
