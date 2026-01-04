# Fix: SQLite 데이터베이스 디렉토리 자동 생성 로직 추가

## 문제 상황
클라우드타입 배포 시 SQLite 데이터베이스 연결 에러 발생:
```
path to './data/scon_dev.db': '/app/./data' does not exist
```

애플리케이션 시작 시 데이터베이스 파일이 저장될 디렉토리가 존재하지 않아 SQLite 연결 실패.

## 해결 방법

### 변경 사항
1. **JpaConfig에 디렉토리 자동 생성 로직 추가**
   - `@PostConstruct` 메서드로 애플리케이션 시작 시 실행
   - `spring.datasource.url`에서 데이터베이스 경로 추출
   - 상대 경로를 절대 경로로 변환
   - 디렉토리가 없으면 자동 생성

### 변경된 파일
- `src/main/java/vibe/scon/scon_backend/config/JpaConfig.java`
  - `ensureDatabaseDirectoryExists()` 메서드 추가
  - `@PostConstruct` 어노테이션으로 초기화 시점에 실행
  - 로깅 추가 (생성 성공/실패, 디버그 로그)

## 구현 상세

### 주요 코드
```java
@PostConstruct
public void ensureDatabaseDirectoryExists() {
    // jdbc:sqlite:./data/scon_dev.db 형식에서 경로 추출
    // 상대 경로를 절대 경로로 변환
    // 디렉토리가 없으면 자동 생성
}
```

### 동작 방식
1. 애플리케이션 시작 시 `@PostConstruct` 메서드 실행
2. `spring.datasource.url`에서 데이터베이스 파일 경로 추출
3. 상대 경로인 경우 현재 작업 디렉토리 기준으로 절대 경로 변환
4. 디렉토리 경로 추출 및 존재 여부 확인
5. 없으면 `Files.createDirectories()`로 생성

## 테스트 결과

### 빌드 테스트 결과
```bash
$ ./gradlew clean build
BUILD SUCCESSFUL in 1m
8 actionable tasks: 8 executed
```
**상태**: ✅ PASSED

### 단위 테스트 실행 결과
```bash
$ ./gradlew test
BUILD SUCCESSFUL in 13s
5 actionable tasks: 5 up-to-date
```
**상태**: ✅ PASSED

**실행된 테스트**:
- ✅ 모든 기존 테스트 케이스 통과
- ✅ JpaConfig 변경사항이 기존 테스트에 영향 없음 확인
- ✅ 데이터베이스 연결 테스트 통과

**테스트 결과 요약**:
- ✅ 통과: 모든 테스트
- ❌ 실패: 0개
- ⚠️ 스킵: 0개

## Git 커밋 정보

### 브랜치
```
feat/owner-schedule-api-and-improvements
```

### 커밋
```
6f6f08f [fix] SQLite 데이터베이스 디렉토리 자동 생성 로직 추가
```

### 변경 통계
```
1 file changed
~40 insertions(+)
~5 deletions(-)
```

## 영향 범위

- **환경**: 모든 환경 (dev, prod, local)
- **영향**: 애플리케이션 시작 시 데이터베이스 디렉토리 자동 생성
- **호환성**: 기존 동작에 영향 없음 (디렉토리가 이미 있으면 스킵)

## 검증 방법

1. 클라우드타입에서 배포
2. 애플리케이션 로그에서 "Created database directory" 메시지 확인
3. 데이터베이스 연결 성공 확인

## 다음 단계

- 클라우드타입 배포 후 동작 확인
- 필요 시 추가 테스트 케이스 작성

---

*Generated: 2026-01-04*
*Branch: feat/owner-schedule-api-and-improvements*

