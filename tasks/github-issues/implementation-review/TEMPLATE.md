# Implementation Report Template

## 사용 방법
이슈 구현 완료 후, 이 템플릿을 복사하여 `{ISSUE-ID}-implementation-report.md` 파일로 저장합니다.

---

## 파일명 규칙
```
SCON-ENV-001-implementation-report.md
SCON-ENV-002-implementation-report.md
REQ-FUNC-001-implementation-report.md
```

---

## 템플릿 구조

```markdown
# Implementation Report: {ISSUE-ID}

## {이슈 제목}

| 항목 | 내용 |
|------|------|
| **Issue ID** | {ISSUE-ID} |
| **GitHub Issue** | [#{번호}](URL) |
| **실행일** | YYYY-MM-DD |
| **커밋 해시** | `{hash}` |
| **상태** | ✅ Completed / ❌ Failed / 🔄 In Progress |

---

## 1. 실행 개요
- 목적, 범위, 적용된 Cursor Rules

## 2. 실행 단계
- Phase별 작업 내용 및 생성 파일

## 3. 테스트 결과
- 빌드 검증, API 테스트, 스크린샷

## 4. 주요 이슈 및 해결 과정
- 테스트/구현 과정에서 발생한 주요 에러 및 해결 방법

### 4.1 이슈 요약표
| # | 이슈 유형 | 원인 | 해결 방법 | 영향 파일 |
|---|----------|------|----------|----------|
| 1 | {에러 메시지 요약} | {발생 원인} | {해결 방법} | {수정 파일} |
| 2 | ... | ... | ... | ... |

### 4.2 상세 이슈 기록 (선택)
#### 이슈 #1: {이슈 제목}
**에러 메시지**:
```
{에러 메시지}
```

**발생 원인**:
- {원인 설명}

**해결 방법**:
```java
// 수정 전
{기존 코드}

// 수정 후
{수정된 코드}
```

**학습 포인트**:
- {이 이슈를 통해 배운 점}

## 5. 생성된 파일 목록
- 소스 코드, 리소스, 설정 파일 테이블

## 6. 완료 조건 검증
- Acceptance Criteria 체크리스트

## 7. Git 커밋 정보
- 커밋 해시, 메시지, 변경 통계

## 8. 다음 단계
- 후속 이슈 안내
```

---

*Generated: {날짜}*

