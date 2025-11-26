---
title: "[REQ-FUNC-006~007] 스케줄 대시보드 및 편집 API 구현"
labels: ["feature", "api", "schedule"]
assignees: []
---

## 1. 목적
스케줄 대시보드 조회 및 드래그 앤 드롭 편집 기능을 지원하는 API를 구현한다.

## 2. 범위
- `REQ-FUNC-006`: 스케줄 상태 조회 (3단계 카드).
- `REQ-FUNC-007`: 스케줄 편집 (임시 저장/Draft 버전 관리).

## 3. 상세 작업
- [ ] `ScheduleController` 구현 (조회, 수정).
- [ ] `ScheduleService` 구현 (Draft vs Approved 버전 처리).
- [ ] `AvailabilitySubmission` 데이터를 스케줄 뷰에 병합하는 로직 구현.
- [ ] 드래그 앤 드롭 액션을 위한 부분 업데이트 지원.

## 4. 완료 조건 (Acceptance Criteria)
- 사장님이 특정 주차의 스케줄 대시보드를 조회할 수 있다.
- 사장님이 근무 시간을 수정하고 임시(Draft) 상태로 저장할 수 있다.
- (권장) 이전 버전의 이력이 보존된다.

## 5. 참고 자료
- `tasks/functional/REQ-FUNC-006-007.md`
