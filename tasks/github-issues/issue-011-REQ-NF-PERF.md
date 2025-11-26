---
title: "[REQ-NF-PERF] 핵심 기능 성능 최적화 및 부하 테스트"
labels: ["nfr", "performance", "test"]
assignees: []
---

## 1. 목적
주요 시나리오(스케줄 조회, 승인, 증빙 생성)에서 목표 응답 시간(Latency)을 달성하도록 시스템을 튜닝하고 검증한다.

## 2. 범위
- `REQ-NF-001`: 스케줄 보드 조회 (p95 ≤ 0.8s).
- `REQ-NF-002`: 승인 처리 (p95 ≤ 1.2s).
- `REQ-NF-003`: 증빙 생성 (≤ 5s).

## 3. 상세 작업
- [ ] DB 인덱스 최적화 (조회 조건 컬럼).
- [ ] JPA N+1 문제 해결 (Fetch Join 등 적용).
- [ ] k6 또는 JMeter를 활용한 부하 테스트 스크립트 작성.
- [ ] 성능 테스트 수행 및 병목 구간 튜닝.

## 4. 완료 조건 (Acceptance Criteria)
- 가상 유저 부하 환경에서 목표 KPI(응답 시간)를 만족해야 한다.
- 부하 테스트 결과 리포트가 작성되어야 한다.

## 5. 참고 자료
- `tasks/non-functional/REQ-NF-PERF.md`

