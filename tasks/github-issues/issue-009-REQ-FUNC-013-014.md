---
title: "[REQ-FUNC-013~014] 증빙 문서 생성 및 관리 구현"
labels: ["feature", "integration", "python"]
assignees: []
---

## 1. 목적
PDF/Excel 증빙 문서를 생성하고 안전하게 저장하는 기능을 구현한다.

## 2. 범위
- `REQ-FUNC-013`: 문서 생성 (Python 마이크로서비스 연동).
- `REQ-FUNC-014`: 해시(Hash) 생성 및 저장.

## 3. 상세 작업
- [ ] Python 문서 생성 서비스를 호출하는 Spring Client 구현.
- [ ] 생성된 파일을 Object Storage(S3 또는 로컬 모의 저장소)에 저장.
- [ ] 파일에 대한 SHA-256 해시를 생성하여 DB에 저장.
- [ ] 생성된 증빙 문서에 대한 다운로드 API 제공.

## 4. 완료 조건 (Acceptance Criteria)
- 요청 시 PDF/Excel 파일이 정상적으로 생성되어야 한다.
- 파일이 안전하게 저장되고, 저장된 파일의 해시값이 일치해야 한다.
- 다운로드 링크가 정상 작동해야 한다.

## 5. 참고 자료
- `tasks/functional/REQ-FUNC-013-014.md`
