---
title: "[REQ-FUNC-008/016] 스케줄 승인 및 알림 발송 구현"
labels: ["feature", "workflow", "notification"]
assignees: []
---

## 1. 목적
스케줄 승인 워크플로우를 구현하고 직원에게 알림을 발송한다.

## 2. 범위
- `REQ-FUNC-008`: 1-Click 승인 액션.
- `REQ-FUNC-016`: 직원 알림 (Kakao/SMS 연동 스텁).

## 3. 상세 작업
- [ ] `approveSchedule` API 구현.
- [ ] `ComplianceService`를 연동하여 위반 발생 시 승인 차단.
- [ ] `NotificationService` 구현 (SMS/Kakao 어댑터 패턴).
- [ ] 스케줄 상태를 `APPROVED`로 변경 및 잠금 처리.

## 4. 완료 조건 (Acceptance Criteria)
- BLOCK 등급의 위반이 있으면 승인이 실패해야 한다.
- 위반이 없으면 승인이 성공하고 스케줄 상태가 변경되어야 한다.
- 승인 시 모의 알림(로그 출력)이 발송되어야 한다.

## 5. 참고 자료
- `tasks/functional/REQ-FUNC-008-016.md`
