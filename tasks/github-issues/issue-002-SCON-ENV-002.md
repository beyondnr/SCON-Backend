---
title: "[SCON-ENV-002] 데이터베이스 스키마 설계 및 설정"
labels: ["database", "schema", "mysql"]
assignees: []
---

## 1. 목적
핵심 도메인 엔터티를 위한 초기 MySQL 데이터베이스 스키마를 설계하고 구현한다.

## 2. 범위
- `Owner`, `Store`, `Employee`, `Schedule`, `Availability` 엔터티 정의.
- MySQL 연결 설정 구성.
- 엔터티 간 관계(OneToMany, ManyToOne) 및 제약조건 설정.

## 3. 상세 작업
- [ ] MySQL 8.x 설치/설정 (로컬 또는 Docker).
- [ ] `Owner` (User) 엔터티 정의.
- [ ] `Store` 엔터티 정의.
- [ ] `Employee` 엔터티 정의.
- [ ] `AvailabilitySubmission` 엔터티 정의.
- [ ] `Schedule` 및 `Shift` 엔터티 정의.
- [ ] `spring.jpa.hibernate.ddl-auto=update/validate` 설정을 통한 DDL 생성 검증.

## 4. 완료 조건 (Acceptance Criteria)
- 애플리케이션 시작 시 DB 연결 오류가 발생하지 않아야 한다.
- MySQL 스키마에 ERD와 일치하는 테이블들이 올바르게 생성되어야 한다.
