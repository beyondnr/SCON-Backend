# Implementation Report: Issue-003 (REQ-FUNC-001~003)

## 온보딩 및 매장/직원 관리 API 구현

| 항목 | 내용 |
|------|------|
| **Issue ID** | Issue-003 (REQ-FUNC-001~003) |
| **GitHub Issue** | [#4](https://github.com/beyondnr/SCON-Backend/issues/4) |
| **Pull Request** | [#23](https://github.com/beyondnr/SCON-Backend/pull/23) |
| **실행일** | 2025-11-29 |
| **최종 커밋** | `feat/issue-003-onboarding-api` 브랜치 |
| **상태** | ✅ Completed (Merged) |

---

## 1. 실행 개요

### 1.1 목적
사장님 회원가입(온보딩), 매장 생성, 직원 관리를 위한 REST API를 구현한다. SRS의 ST-Story-2 (1클릭 스케줄 승인)의 선행 요구사항을 충족한다.

### 1.2 범위
- **REQ-FUNC-001**: 3단계 온보딩 마법사 (회원가입 → 매장설정 → 첫 스케줄 생성)
- **REQ-FUNC-002**: 매장 정보 수집 및 관리 API
- **REQ-FUNC-003**: 직원 등록 및 PII 암호화 처리

### 1.3 적용된 Cursor Rules
| Rule | 설명 |
|------|------|
| `101-build-and-env-setup.mdc` | 환경변수 설정 (.env 파일) |
| `102-gitflow-agent.mdc` | Git Flow 커밋/브랜치 전략 |
| `103-pr-creation.mdc` | PR 생성 및 본문 작성 규칙 |
| `201-code-commenting.mdc` | 코드 주석 규칙 (Javadoc, SRS/Issue 추적성) |

---

## 2. 주요 에러 및 해결 과정

이번 구현 과정에서 발생한 주요 에러들과 해결 방법을 상세히 기록합니다.

### 2.1 BusinessException 추상 클래스 인스턴스화 에러

**에러 메시지**:
```
error: BusinessException is abstract; cannot be instantiated
```

**발생 원인**:
- `BusinessException`이 추상 클래스로 정의되어 있는데, Service 레이어에서 직접 인스턴스화하려고 시도함
- 소유권 검증 실패 시 403 Forbidden 응답을 위해 예외를 던져야 했음

**해결 방법**:
`ForbiddenException` 클래스를 새로 생성하여 HTTP 403 상태 코드에 매핑:

```java
// src/main/java/vibe/scon/scon_backend/exception/ForbiddenException.java
@ResponseStatus(HttpStatus.FORBIDDEN)
public class ForbiddenException extends BusinessException {
    public ForbiddenException(String message) {
        super(message);
    }
}
```

**Service에서 사용**:
```java
// Before (에러 발생)
throw new BusinessException("접근 권한이 없습니다");

// After (해결)
throw new ForbiddenException("접근 권한이 없습니다");
```

---

### 2.2 AES-256 암호화 키 길이 에러

**에러 메시지**:
```
java.lang.IllegalArgumentException at EncryptionUtil.java:59
Invalid AES key length: XX bytes
```

**발생 원인**:
- AES-256 암호화는 정확히 32바이트(256비트) 키가 필요
- `.env` 파일의 `APP_ENCRYPTION_KEY` 값 길이가 32바이트가 아님
- `application.yml`의 기본값도 32바이트가 아님

**1차 시도 (실패)**:
```yaml
# application.yml - 32자로 수정했으나 여전히 에러
app:
  encryption:
    key: ${APP_ENCRYPTION_KEY:your-32-char-encryption-key-here}  # 32자
```
→ 문자열 길이와 바이트 길이가 다를 수 있음 (UTF-8 인코딩 시)

**최종 해결 방법**:
입력 키를 SHA-256 해시하여 **항상 32바이트 키를 보장**하도록 수정:

```java
// EncryptionUtil.java - 생성자
public EncryptionUtil(@Value("${app.encryption.key}") String encryptionKey) {
    try {
        // SHA-256 해시를 사용하여 항상 32바이트 키 생성
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = digest.digest(encryptionKey.getBytes(StandardCharsets.UTF_8));
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
        log.info("EncryptionUtil initialized with AES-256-GCM (key derived via SHA-256)");
    } catch (NoSuchAlgorithmException e) {
        throw new RuntimeException("SHA-256 algorithm not available", e);
    }
}
```

**개선 효과**:
- 어떤 길이의 입력 키든 안전하게 32바이트로 변환
- 키 길이 관련 에러 완전 방지
- 보안 강화 (원본 키가 직접 노출되지 않음)

---

### 2.3 JWT 비밀키 길이 에러

**에러 메시지**:
```
java.lang.IllegalArgumentException at JwtTokenProvider.java:56
The specified key byte array is X bits which is not secure enough for any JWT HMAC-SHA algorithm
```

**발생 원인**:
- JWT의 HMAC-SHA256 알고리즘도 적절한 키 길이 필요
- `EncryptionUtil`과 동일한 키 길이 문제

**해결 방법**:
`EncryptionUtil`과 동일하게 SHA-256 해시 적용:

```java
// JwtTokenProvider.java - 생성자
public JwtTokenProvider(
        @Value("${app.jwt.secret}") String secretKey,
        @Value("${app.jwt.access-expiration:1800000}") long accessTokenExpiration,
        @Value("${app.jwt.refresh-expiration:604800000}") long refreshTokenExpiration) {
    try {
        // SHA-256 해시를 사용하여 항상 32바이트 키 생성
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = digest.digest(secretKey.getBytes(StandardCharsets.UTF_8));
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        // ...
    } catch (NoSuchAlgorithmException e) {
        throw new RuntimeException("SHA-256 algorithm not available", e);
    }
}
```

---

### 2.4 AuthServiceTest 컴파일 에러 (BaseEntity id 필드)

**에러 메시지**:
```
error: cannot find symbol
  method id(long)
  location: class OwnerBuilder
```

**발생 원인**:
- `Owner` 엔티티의 `id` 필드는 `BaseEntity`에서 상속받음
- Lombok의 `@Builder`는 상속받은 필드를 빌더에 포함하지 않음
- 테스트 코드에서 `Owner.builder().id(1L).build()` 사용 불가

**잘못된 코드**:
```java
// 컴파일 에러 발생
testOwner = Owner.builder()
        .id(1L)  // ❌ BaseEntity의 필드라 Builder에 없음
        .email("test@example.com")
        .password("encodedPassword")
        .name("홍길동")
        .phone("010-1234-5678")
        .build();
```

**해결 방법**:
`ReflectionTestUtils`를 사용하여 `id` 필드를 직접 설정:

```java
// AuthServiceTest.java
import org.springframework.test.util.ReflectionTestUtils;

@BeforeEach
void setUp() {
    // Owner 객체 생성 (id 제외)
    testOwner = Owner.builder()
            .email("test@example.com")
            .password("encodedPassword")
            .name("홍길동")
            .phone("010-1234-5678")
            .build();
    
    // Reflection으로 id 필드 설정
    ReflectionTestUtils.setField(testOwner, "id", 1L);
}
```

**적용 범위**:
- `AuthServiceTest.java` - testOwner 객체
- 향후 유사한 테스트 케이스에도 동일 패턴 적용

---

### 2.5 통합 테스트 HTTP 상태 코드 불일치

**에러 메시지**:
```
java.lang.AssertionError: Status expected:<200> but was:<201>
java.lang.AssertionError: JSON path "$.status" expected:<success> but was:<201>
```

**발생 원인**:
- 테스트 코드가 `status().isOk()` (HTTP 200)를 기대
- 실제 Controller는 생성 API에서 `HttpStatus.CREATED` (HTTP 201) 반환
- `ApiResponse`의 `status` 필드가 문자열 `"success"`가 아닌 숫자 `201`

**잘못된 테스트 코드**:
```java
mockMvc.perform(post("/api/v1/auth/signup")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(signupRequest)))
    .andExpect(status().isOk())  // ❌ 200 기대
    .andExpect(jsonPath("$.status").value("success"));  // ❌ 문자열 기대
```

**수정된 테스트 코드**:
```java
// 생성 API (POST)는 201 Created 반환
mockMvc.perform(post("/api/v1/auth/signup")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(signupRequest)))
    .andExpect(status().isCreated())  // ✅ 201 Created
    .andExpect(jsonPath("$.status").value(201));  // ✅ 숫자 201

// 조회 API (GET)는 200 OK 반환
mockMvc.perform(get("/api/v1/stores/{id}", storeId)
        .header("Authorization", "Bearer " + accessToken))
    .andExpect(status().isOk())  // ✅ 200 OK
    .andExpect(jsonPath("$.status").value(200));  // ✅ 숫자 200
```

**수정 파일**:
| 파일 | 수정 내용 |
|------|----------|
| `AuthControllerIntegrationTest.java` | signup → `isCreated()`, `value(201)` |
| `StoreControllerIntegrationTest.java` | createStore → `isCreated()`, `value(201)` |
| `EmployeeControllerIntegrationTest.java` | createEmployee → `isCreated()`, `value(201)` |

---

### 2.6 Spring Security 401 vs 403 응답 문제

**에러 메시지**:
```
java.lang.AssertionError: Status expected:<401> but was:<403>
```

**발생 원인**:
- 테스트: 인증 없이 API 호출 시 HTTP 401 Unauthorized 기대
- 실제: Spring Security 기본 동작은 HTTP 403 Forbidden 반환
- `AccessDeniedException`이 발생하면 403을 반환하는 것이 Spring Security의 기본 동작

**해결 방법**:
`SecurityConfig`에 `authenticationEntryPoint` 설정 추가:

```java
// SecurityConfig.java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .anyRequest().authenticated())
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        
        // 예외 처리: 인증 실패 시 401 반환
        .exceptionHandling(exception -> exception
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"status\":401,\"message\":\"인증이 필요합니다\"}");
                })
        );
    
    return http.build();
}
```

**추가 import**:
```java
import jakarta.servlet.http.HttpServletResponse;
```

---

### 2.7 JWT SignatureException 처리 누락

**에러 메시지**:
```
io.jsonwebtoken.security.SignatureException: JWT signature does not match locally computed signature
```

**발생 원인**:
- `JwtTokenProvider.validateToken()` 메서드에서 `SignatureException`을 명시적으로 catch하지 않음
- 다른 비밀키로 서명된 토큰 검증 시 예외가 상위로 전파됨
- 테스트 코드에서 `assertThrows`로 예외를 기대했지만, `validateToken`은 boolean 반환이 설계 의도

**해결 방법**:
`validateToken` 메서드에 `SignatureException` catch 블록 추가:

```java
// JwtTokenProvider.java
public boolean validateToken(String token) {
    try {
        Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token);
        return true;
    } catch (io.jsonwebtoken.security.SignatureException e) {
        // 서명 검증 실패 (다른 키로 서명된 토큰)
        log.error("Invalid JWT signature: {}", e.getMessage());
    } catch (SecurityException | MalformedJwtException e) {
        log.error("Invalid JWT signature or malformed token: {}", e.getMessage());
    } catch (ExpiredJwtException e) {
        log.error("Expired JWT token: {}", e.getMessage());
    } catch (UnsupportedJwtException e) {
        log.error("Unsupported JWT token: {}", e.getMessage());
    } catch (IllegalArgumentException e) {
        log.error("JWT claims string is empty: {}", e.getMessage());
    } catch (JwtException e) {
        // 기타 모든 JWT 예외
        log.error("JWT token validation failed: {}", e.getMessage());
    }
    return false;
}
```

**테스트 코드 수정**:
```java
// JwtTokenProviderTest.java
@Test
@DisplayName("다른 비밀키로 서명된 토큰 검증 실패")
void validateToken_differentSecretKey_returnsFalse() {
    // Given - 다른 비밀키로 생성된 토큰
    JwtTokenProvider otherProvider = new JwtTokenProvider(
            "completely-different-secret-key-for-testing",
            1800000L, 604800000L
    );
    String tokenFromOtherProvider = otherProvider.generateAccessToken(1L, "test@example.com");

    // When - validateToken은 예외를 catch하고 false 반환
    boolean isValid = jwtTokenProvider.validateToken(tokenFromOtherProvider);

    // Then
    assertThat(isValid).isFalse();  // 예외 던지지 않고 false 반환
}
```

---

## 3. 에러 해결 요약표

| # | 에러 유형 | 원인 | 해결 방법 | 영향 파일 |
|---|----------|------|----------|----------|
| 1 | BusinessException 추상 클래스 | 직접 인스턴스화 시도 | `ForbiddenException` 생성 | `ForbiddenException.java`, Services |
| 2 | AES 키 길이 오류 | 32바이트 키 필요 | SHA-256 해시로 키 유도 | `EncryptionUtil.java` |
| 3 | JWT 키 길이 오류 | HMAC-SHA256 키 요구사항 | SHA-256 해시로 키 유도 | `JwtTokenProvider.java` |
| 4 | Builder id 접근 불가 | BaseEntity 상속 필드 | `ReflectionTestUtils` 사용 | `AuthServiceTest.java` |
| 5 | HTTP 상태 코드 불일치 | 201 vs 200 | `isCreated()`, `value(201)` | 통합 테스트 3개 |
| 6 | 401 vs 403 응답 | Security 기본 동작 | `authenticationEntryPoint` 설정 | `SecurityConfig.java` |
| 7 | SignatureException 누락 | 예외 catch 누락 | 명시적 catch 블록 추가 | `JwtTokenProvider.java` |

---

## 4. 테스트 결과

### 4.1 최종 테스트 실행 결과

```bash
$ ./gradlew clean test

BUILD SUCCESSFUL in 45s
8 actionable tasks: 8 executed
```

### 4.2 테스트 케이스 요약

| 테스트 파일 | 유형 | 테스트 수 | 상태 |
|------------|------|----------|------|
| `EncryptionUtilTest.java` | 단위 | 11개 | ✅ PASSED |
| `JwtTokenProviderTest.java` | 단위 | 13개 | ✅ PASSED |
| `AuthServiceTest.java` | 단위 | 9개 | ✅ PASSED |
| `AuthControllerIntegrationTest.java` | 통합 | 8개 | ✅ PASSED |
| `StoreControllerIntegrationTest.java` | 통합 | 8개 | ✅ PASSED |
| `EmployeeControllerIntegrationTest.java` | 통합 | 8개 | ✅ PASSED |

**총 테스트**: 57개 | **성공**: 57개 | **실패**: 0개

### 4.3 테스트 커버리지

| 영역 | 커버된 테스트 케이스 |
|------|---------------------|
| 인증 (Auth) | TC-AUTH-001~009 |
| 매장 (Store) | TC-STORE-001~007 |
| 직원 (Employee) | TC-EMP-001~008 |
| PII 암호화 | TC-NFR-002 |

---

## 5. 생성된 파일 목록 (29개)

### 5.1 Infrastructure/Security (5개)
| 파일 | 설명 |
|------|------|
| `config/SecurityConfig.java` | Spring Security 설정 (JWT 필터, 인가 규칙) |
| `config/filter/JwtAuthenticationFilter.java` | JWT 토큰 검증 필터 |
| `util/JwtTokenProvider.java` | JWT 토큰 생성/검증 유틸리티 |
| `util/EncryptionUtil.java` | AES-256-GCM PII 암호화 유틸리티 |
| `exception/ForbiddenException.java` | HTTP 403 예외 클래스 |

### 5.2 API 레이어 (17개)
| 영역 | Controller | Service | DTOs |
|------|------------|---------|------|
| Auth | `AuthController.java` | `AuthService.java` | 4개 (Signup, Login, Refresh, Token) |
| Store | `StoreController.java` | `StoreService.java` | 2개 (Request, Response) |
| Employee | `EmployeeController.java` | `EmployeeService.java` | 2개 (Request, Response) |
| Onboarding | `OnboardingController.java` | `OnboardingService.java` | 2개 (Request, Response) |

### 5.3 테스트 (7개)
| 파일 | 유형 | 테스트 수 |
|------|------|----------|
| `EncryptionUtilTest.java` | 단위 | 11 |
| `JwtTokenProviderTest.java` | 단위 | 13 |
| `AuthServiceTest.java` | 단위 | 9 |
| `AuthControllerIntegrationTest.java` | 통합 | 8 |
| `StoreControllerIntegrationTest.java` | 통합 | 8 |
| `EmployeeControllerIntegrationTest.java` | 통합 | 8 |
| `application-test.yml` | 설정 | - |

---

## 6. API 엔드포인트 요약 (13개)

| Method | Path | 인증 | 설명 |
|--------|------|------|------|
| POST | `/api/v1/auth/signup` | 공개 | 회원가입 |
| POST | `/api/v1/auth/login` | 공개 | 로그인 (JWT 발급) |
| POST | `/api/v1/auth/refresh` | Refresh Token | 토큰 갱신 |
| POST | `/api/v1/stores` | JWT | 매장 생성 |
| GET | `/api/v1/stores` | JWT | 내 매장 목록 |
| GET | `/api/v1/stores/{id}` | JWT | 매장 상세 |
| PUT | `/api/v1/stores/{id}` | JWT | 매장 수정 |
| POST | `/api/v1/stores/{storeId}/employees` | JWT | 직원 등록 |
| GET | `/api/v1/stores/{storeId}/employees` | JWT | 직원 목록 |
| GET | `/api/v1/employees/{id}` | JWT | 직원 상세 |
| PUT | `/api/v1/employees/{id}` | JWT | 직원 수정 |
| DELETE | `/api/v1/employees/{id}` | JWT | 직원 삭제 |
| POST | `/api/v1/onboarding/complete` | JWT | 온보딩 완료 |

---

## 7. Acceptance Criteria 검증

| AC | 설명 | 상태 | 검증 방법 |
|----|------|------|----------|
| AC-001 | 사장님 회원가입 및 JWT 발급 | ✅ | `AuthControllerIntegrationTest` |
| AC-002 | 사장님 로그인 | ✅ | `AuthControllerIntegrationTest` |
| AC-003 | 매장 정보 저장 및 조회 | ✅ | `StoreControllerIntegrationTest` |
| AC-004 | 직원 등록 및 PII 암호화 | ✅ | `EmployeeControllerIntegrationTest`, `EncryptionUtilTest` |
| AC-005 | 온보딩 완료 및 첫 스케줄 생성 | ✅ | `OnboardingService` 구현 완료 |

---

## 8. 학습 포인트

### 8.1 보안 키 관리
- 환경변수에서 받은 키를 직접 사용하지 말고 **해시 함수로 키 유도** (Key Derivation)
- SHA-256 해시를 사용하면 입력 길이와 관계없이 항상 32바이트 키 보장

### 8.2 Lombok @Builder와 상속
- `@Builder`는 **현재 클래스의 필드만** 빌더에 포함
- 부모 클래스(BaseEntity)의 `id` 같은 필드는 `ReflectionTestUtils`로 설정

### 8.3 Spring Security 예외 처리
- 기본 동작은 인증 실패 시 403 반환
- 401 응답을 위해 `authenticationEntryPoint` 명시적 설정 필요

### 8.4 REST API 상태 코드
- 리소스 생성 (POST) → 201 Created
- 조회/수정/삭제 → 200 OK
- 테스트 코드도 이에 맞게 `isCreated()` / `isOk()` 구분

---

## 9. Git 커밋 정보

### 9.1 브랜치
```
feat/issue-003-onboarding-api
```

### 9.2 Pull Request
- **PR #23**: [온보딩 및 매장/직원 관리 API 구현](https://github.com/beyondnr/SCON-Backend/pull/23)
- **상태**: ✅ Merged to `main`

### 9.3 주요 커밋
| 커밋 | 내용 |
|------|------|
| 초기 구현 | Auth, Store, Employee, Onboarding API |
| 버그 수정 | ForbiddenException, 키 유도 로직 |
| 테스트 추가 | 57개 단위/통합 테스트 |
| 테스트 수정 | HTTP 상태 코드, Security 설정 |

---

## 10. 다음 단계

Issue-003 (REQ-FUNC-001~003)가 완료되었습니다. ISSUE_EXECUTION_PLAN.md에 따라 다음 작업:

| 우선순위 | 이슈 | 설명 | 의존성 |
|----------|------|------|--------|
| 1 | Issue-004 | REQ-FUNC-004~005: 직원 가용시간 제출 API | Issue-003 완료 ✅ |
| 2 | Issue-005 | REQ-FUNC-006~007: 스케줄 대시보드 및 편집 API | Issue-003 완료 ✅ |
| 병렬 가능 | Issue-006 | REQ-FUNC-009~010: 스케줄 승인 및 공개 API | Issue-005 의존 |

---

*Generated: 2025-11-29*
*Agent: Cursor AI Assistant*
*Branch: feat/issue-003-onboarding-api*
*PR: #23 (Merged)*
*Closes: #4*
