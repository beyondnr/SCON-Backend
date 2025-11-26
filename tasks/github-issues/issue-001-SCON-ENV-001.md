---
title: "[SCON-ENV-001] 프로젝트 초기화 및 개발 환경 구성"
labels: ["setup", "backend", "configuration"]
assignees: []
---

## 1. 목적
Spring Boot 3.x 프로젝트 구조를 초기화하고 개발을 위한 기본 환경을 구성한다.

## 2. 범위
- Spring Boot 3.x 기반의 Gradle 프로젝트 설정.
- 패키지 구조 정의 (`vibe.scon.scon_backend`).
- `application.properties` (또는 `.yml`) 프로파일 설정 (dev, prod).
- 전역 예외 처리기(Global Exception Handler) 및 공통 응답(ApiResponse) 래퍼 구현.

## 3. 상세 작업
- [ ] Spring Boot 프로젝트 생성 (Java 17, Gradle).
- [ ] 패키지 구조 정의 (controller, service, repository, entity, dto 등).
- [ ] `.gitignore` 및 `README.md` 설정.
- [ ] `GlobalExceptionHandler` 구현 (@RestControllerAdvice).
- [ ] 표준 `ApiResponse<T>` 클래스 정의.

## 4. 완료 조건 (Acceptance Criteria)
- `./gradlew build` 명령어로 프로젝트가 성공적으로 빌드되어야 한다.
- API 호출 시 성공/실패 응답이 표준 JSON 포맷으로 반환되어야 한다.
