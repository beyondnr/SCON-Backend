# REQ-FUNC-013~014: 증빙 생성 및 보관 서비스 구현

## 1. 목적 및 요약
* **목적**: 계산된 급여 내역과 스케줄 정보를 법적 효력이 있는(또는 참고용) 문서 형태로 생성하고, 안전하게 보관한다.
* **요약**: HTML/PDF 변환 로직(또는 라이브러리 사용)과 S3(또는 로컬 스토리지) 업로드, 그리고 해시 생성/저장 기능을 구현한다.

## 2. 관련 REQ 및 엔티티
* **관련 REQ**: `REQ-FUNC-013` (증빙 생성), `REQ-FUNC-014` (해시/보관)
* **관련 엔티티**: `EvidenceFile`, `PayrollRecord`

## 3. 주요 기능 및 처리 단계
1. **문서 생성**: 급여 명세서 데이터를 받아 PDF(iText, Flying Saucer 등) 또는 HTML 파일 생성.
2. **해시 생성**: 생성된 파일의 SHA-256 체크섬 계산.
3. **업로드**: 파일을 오브젝트 스토리지(S3 Mock 또는 MinIO)에 업로드.
4. **메타데이터 저장**: 파일 URL, 해시값, 생성 시각을 DB에 저장.
5. **다운로드 링크 제공**: 서명된 URL(Presigned URL) 또는 Proxy 다운로드 API 제공.

## 4. API 명세 (개요)
* `POST /api/v1/reports/payroll/export`: 증빙 생성 요청.
* `GET /api/v1/evidence/{id}/download`: 다운로드.

---

```yaml
task_id: "REQ-FUNC-013-014"
title: "증빙 생성 및 보관 서비스 구현"
summary: >
  급여/스케줄 증빙 문서를 PDF/HTML로 생성하고, SHA-256 해시와 함께
  스토리지에 저장하는 기능을 구현한다.
type: "functional"

epic: "E4_PAYROLL"
req_ids: ["REQ-FUNC-013", "REQ-FUNC-014"]
component: ["backend.service", "document-engine"]

context:
  srs_section: "4.1 Functional Requirements"
  entities: ["EvidenceFile"]

inputs:
  description: >
    급여 리포트 데이터.
  fields:
    - name: "payroll_data"
      type: "object"

outputs:
  description: >
    생성된 증빙 파일 정보.
  success:
    http_status: 201
    body: "{ id: '...', url: '...', hash: 'sha256:...' }"

steps_hint:
  - "PDF 생성 라이브러리(OpenPDF 등) 또는 HTML 템플릿 엔진(Thymeleaf) 설정."
  - "FileStorageService 인터페이스 및 S3/Local 구현체 작성."
  - "파일 생성 직후 SHA-256 해싱 로직 수행."
  - "DB에 메타데이터 저장."

preconditions:
  - "Payroll 데이터가 준비되어 있어야 함."

postconditions:
  - "생성된 파일이 스토리지에 존재하고, DB 해시값과 실제 파일 해시값이 일치해야 함."

tests:
  unit:
    - "파일 해시 계산 정확성 테스트"
  integration:
    - "증빙 생성 요청 -> 파일 저장 -> 다운로드 가능 여부 확인"

dependencies: ["REQ-FUNC-011-012"]

parallelizable: true
estimated_effort: "M"
priority: "Must"
agent_profile: ["backend", "infra"]

required_tools:
  languages: ["Java", "Python(Optional)"]
  frameworks: ["Spring Boot 3.x"]
  infra: ["S3 (Mock/MinIO)"]

references:
  srs: ["REQ-FUNC-013", "REQ-FUNC-014"]

risk_notes:
  - "PDF 생성은 리소스를 많이 소모하므로 비동기 처리 고려."

example_commands:
  - "REQ-FUNC-013-014 Task에 따라 증빙 생성 서비스를 구현해줘."
```

