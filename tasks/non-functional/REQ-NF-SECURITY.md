# REQ-NF-SECURITY: 보안 및 데이터 보호 구현

## 1. 목적 및 요약
* **목적**: 개인정보(PII) 및 중요 데이터를 보호하고, 보안 위협으로부터 시스템을 방어한다.
* **요약**: 데이터 암호화(At-Rest), 전송 구간 암호화(In-Transit), 접근 제어, PII 최소 수집 원칙을 시스템에 적용한다.

## 2. 관련 REQ
* `REQ-NF-007`: 저장 데이터 암호화 (AES-256)
* `REQ-NF-008`: 전송 구간 암호화 (TLS 1.3)
* `REQ-NF-009`: 데이터 국외 반출 금지 (국내 리전 사용)
* `REQ-NF-010`: PII 최소 수집 (8개 이하)
* `REQ-NF-011`: 관리자 접근 제어 (인증/MFA)

## 3. 요구사항 상세
* **암호화**: 연락처, 계좌번호 등 민감 정보는 DB 저장 시 암호화(AttributeConverter 활용).
* **HTTPS**: 로드밸런서 또는 웹서버 레벨에서 TLS 적용, HTTP -> HTTPS 리다이렉트.
* **접근 제어**: Spring Security 적용, 관리자 페이지는 IP 제한 또는 추가 인증 고려.

## 4. 구현 및 검증 활동
1. **AES-256 Converter**: JPA 엔터티 저장/조회 시 투명하게 암호화/복호화 수행.
2. **Security Config**: CSRF 보호, CORS 설정, 세션 관리 등 기본 보안 설정.
3. **인프라 설정**: AWS 리전 확인(Seoul), ACM 인증서 적용.

---

```yaml
task_id: "REQ-NF-SECURITY"
title: "데이터 암호화 및 보안 설정 적용"
summary: >
  PII 암호화 저장, TLS 적용, 접근 제어 등
  서비스 보안 및 규제 준수 요구사항을 구현한다.
type: "non_functional"

epic: "E9_NFR"
req_ids: ["REQ-NF-007", "REQ-NF-008", "REQ-NF-009", "REQ-NF-010", "REQ-NF-011"]
component: ["backend.security", "infra"]

category: "security"
labels:
  - "security:encryption"
  - "security:compliance"

context:
  srs_section: "4.2 Non-Functional Requirements"
  related_req_func: ["REQ-FUNC-001-003"]

requirements:
  description: >
    사용자 정보 보호 및 보안 표준 준수.
  kpis:
    - "모든 PII 컬럼 암호화 저장"
    - "SSL Labs 등급 A 이상"

implementation_scope:
  includes:
    - "AES-256 Encryption Util & JPA Converter"
    - "Spring Security 설정"
    - "Infra 리전 확인"

steps_hint:
  - "암호화 키(Encryption Key)는 환경변수 또는 KMS로 주입받도록 설계."
  - "API 엔드포인트에 대해 인증(Authentication) 및 인가(Authorization) 테스트."
  - "Employee 엔터티 필드 개수 점검 (PII 최소화)."

preconditions:
  - "기본 엔터티 설계 완료."

postconditions:
  - "DB 직접 조회 시 평문 PII가 보이지 않아야 함."

tests:
  - "DB 조회 툴로 암호화된 데이터 확인"
  - "HTTP 요청 시 HTTPS 리다이렉트 확인"

dependencies: ["REQ-FUNC-001-003"]

parallelizable: true
estimated_effort: "M"
priority: "Must"
agent_profile: ["backend", "security"]

required_tools:
  languages: ["Java"]
  frameworks: ["Spring Security"]

references:
  srs: ["REQ-NF-007", "REQ-NF-008"]

risk_notes:
  - "암호화 키 분실 시 데이터 복구 불가 -> 키 백업 프로세스 중요."

```

