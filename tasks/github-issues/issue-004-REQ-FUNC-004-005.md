---
title: "[REQ-FUNC-004~005] 직원 가용시간 제출 API 및 검증 로직"
labels: ["feature", "api", "availability"]
assignees: []
---

## 1. 목적
직원들이 근무 가능 시간을 제출하는 API를 구현하고, 중복 시간 등에 대한 유효성을 검증한다.

## 2. 범위
- `REQ-FUNC-004`: 가용시간 제출용 공개 API (토큰 기반).
- `REQ-FUNC-005`: 검증 로직 (시간 중복 체크).
- `REQ-FUNC-018`: 검증 실패 시 승인 차단 연계 (기초 작업).

## 3. 상세 작업
- [ ] `AvailabilityController` 구현 (토큰을 이용한 접근).
- [ ] `AvailabilityService` 구현 (저장/수정 로직).
- [ ] `TimeRangeValidator` 구현 (중복된 근무 시간대 감지).
- [ ] 검증 오류 시 400 Bad Request 처리.

## 4. 완료 조건 (Acceptance Criteria)
- 직원이 토큰 URL을 통해 가용시간을 제출할 수 있다.
- 시간이 겹치는 제출 요청은 거부된다.
- 유효한 제출 내역은 `AvailabilitySubmission` 테이블에 저장된다.

## 5. 참고 자료
- `tasks/functional/REQ-FUNC-004-005.md`
