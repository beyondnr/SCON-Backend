# SCON (Shift Control) - Backend

**SCON(Shift Control)**은 베이커리, 카페 등 소규모 사업장을 위한 **SaaS형 직원 스케줄 및 준법 급여 관리 솔루션**입니다.  
사장님의 스케줄 관리 부담을 줄이고, 복잡한 노동법 준수 여부를 자동으로 체크하여 임금 체불 및 과태료 위험을 최소화하는 것을 목표로 합니다.

---

## 🚀 Project Overview

SCON은 다음과 같은 핵심 가치를 제공합니다:
- **Efficiency**: 직관적인 UI와 자동화를 통해 스케줄링 시간을 50% 이상 단축
- **Accuracy**: 급여 계산 오류 제거 및 스케줄 충돌 방지
- **Compliance**: 주 52시간제, 휴게시간 등 노동법 위반 사항 자동 감지 및 가이드 제공
- **Reliability**: 데이터 무결성 보장 및 증빙 자료(PDF/Excel) 자동 생성/보관

## ✨ Key Features (MVP v1.0)

1. **3단계 온보딩 마법사 (Onboarding Wizard)**
   - 사장님 회원가입, 매장 정보 설정, 첫 스케줄 생성까지 간소화된 프로세스 제공

2. **직원 가용시간 제출 (Availability Submission)**
   - 직원이 모바일 웹 폼을 통해 근무 가능 시간을 손쉽게 제출
   - 제출된 시간의 누락 및 중복 자동 검증

3. **1-Click 스케줄 승인 (Smart Schedule Approval)**
   - 사장님 대시보드에서 '검토 → 조정 → 공지'의 3단계 흐름 제공
   - 규칙 위반이 없을 경우 단 한 번의 클릭으로 스케줄 확정 및 직원 통지

4. **노동법 규칙 엔진 (Compliance Engine)**
   - 스케줄 승인 시 주 52시간, 휴게시간 등 법규 위반 여부 자동 체크
   - 위반 시 승인 차단 및 구체적인 수정 가이드 제공

5. **자동 급여 계산 및 증빙 생성 (Payroll & Evidence)**
   - 근무 시간 기반 기본급, 주휴/연장/야간/휴일 수당 자동 계산
   - 급여 대장 및 증빙 문서(PDF/Excel) 자동 생성 및 해시(Hash) 기반 위변조 방지

6. **변경 이력 관리 (Audit Log)**
   - 스케줄, 규칙, 급여 데이터의 모든 변경 사항(누가, 언제, 무엇을) 기록

## 🛠 Tech Stack

### Backend Core
- **Language**: Java 21 (LTS)
- **Framework**: Spring Boot 3.x
- **Build Tool**: Gradle
- **Database**: SQLite (파일 기반)
- **ORM**: Spring Data JPA (Hibernate)

### Architecture & Integration
- **Architecture**: Monolithic Service (MVP Phase)
- **API Style**: RESTful API (JSON) with versioning (`/api/v1/...`)
- **API Documentation**: Swagger/OpenAPI 3.0 (SpringDoc)
- **Document Gen**: Python + FastAPI + LangChain (Microservice)
- **External**: KakaoTalk/SMS Gateway, OpenAI/Gemini (LLM for future expansion)

## 📂 Project Structure

```text
src/main/java/vibe/scon/scon_backend
├── config/             # Spring Configuration
│   └── properties/     # @ConfigurationProperties 클래스
├── controller/         # REST Controllers
├── service/            # Business Logic
├── repository/         # Data Access Layer (JPA)
├── entity/             # JPA Entities (Domain)
│   └── enums/          # Enum 타입 정의
├── dto/                # Data Transfer Objects
├── exception/          # Global Exception Handling
└── util/               # Utility Classes
```

---

## 🔐 Security & Environment Management

### 보안 관리 방침

SCON 백엔드는 민감한 정보를 안전하게 관리하기 위해 다음과 같은 보안 체계를 적용합니다.

#### 환경변수 카테고리

| 카테고리 | 환경변수 | 보안 등급 | 설명 |
|----------|----------|-----------|------|
| **Database** | `SQLITE_DB_PATH` | 🟡 Medium | SQLite 데이터베이스 파일 경로 (선택) |
| **JWT** | `JWT_SECRET_KEY` | 🔴 Critical | 인증 토큰 서명 키 (최소 32자) |
| **Encryption** | `ENCRYPTION_KEY` | 🔴 Critical | PII 암호화 키 (AES-256) |
| **AI APIs** | `OPENAI_API_KEY`, `GEMINI_API_KEY` | 🔴 Critical | AI 모델 API 키 |
| **Notification** | `KAKAO_API_KEY`, `SMS_API_KEY` | 🔴 Critical | 알림 서비스 API 키 |
| **Storage** | `AWS_*` | 🔴 Critical | AWS S3 접근 자격증명 |

#### 암호화 스펙

| 항목 | 스펙 | 용도 |
|------|------|------|
| **비밀번호 해시** | BCrypt (cost factor: 12) | Owner 비밀번호 저장 |
| **PII 암호화** | AES-256-GCM | Employee 전화번호 등 민감정보 |
| **JWT 서명** | HS256 | Access/Refresh 토큰 |
| **전송 암호화** | TLS 1.3 (목표) | 모든 외부 통신 |

#### 프로파일별 보안 정책

| 프로파일 | 환경변수 누락 시 | 기본값 허용 | 설명 |
|----------|------------------|-------------|------|
| `local` | ⚠️ 경고 후 계속 | ✅ 허용 | 로컬 개발용 |
| `dev` | ⚠️ 경고 후 계속 | ✅ 허용 | 개발 서버 |
| `staging` | ❌ 앱 시작 차단 | ❌ 불허 | 스테이징 환경 |
| `prod` | ❌ 앱 시작 차단 | ❌ 불허 | 프로덕션 환경 |

#### 환경변수 검증

앱 시작 시 `EnvironmentValidator` 컴포넌트가 필수 환경변수를 자동 검증합니다:

```
========================================
  Environment Validation
  Active Profile: local
========================================
  ✓ JWT_SECRET_KEY: configured (52 chars)
  ✓ ENCRYPTION_KEY: configured (36 chars)
  ✓ OPENAI_API_KEY: configured (model: gpt-3.5-turbo)
  ○ GEMINI_API_KEY: not configured (optional)
  ○ Storage: local mode (path: ./storage)
----------------------------------------
  ✓ All required environment variables are configured
========================================
```

### Git에서 제외되는 파일

다음 파일들은 `.gitignore`에 의해 Git 추적에서 제외됩니다:

```
.env              # 환경변수 파일
.env.local        # 로컬 환경변수
*.key             # 개인키 파일
*.pem             # 인증서 파일
secrets/          # 시크릿 디렉토리
storage/          # 로컬 스토리지
```

---

## ⚡ Getting Started

### Prerequisites
- JDK 21 이상
- Docker & Docker Compose
- Git

### 1. Clone & Setup

```bash
# 저장소 클론
git clone <repository-url>
cd SCON-Backend

# 환경변수 파일 생성
cp .env.example .env
```

### 2. 환경변수 설정 (.env)

`.env` 파일을 열어 실제 값으로 수정합니다:

```properties
# =============================================================================
# DATABASE (SQLite - 선택)
# =============================================================================
# SQLite 데이터베이스 파일 경로 (기본값: ./data/scon_local.db)
# SQLITE_DB_PATH=./data/scon_local.db

# =============================================================================
# SECURITY (프로덕션에서 필수)
# =============================================================================
# JWT 키 생성: openssl rand -base64 32
JWT_SECRET_KEY=your_jwt_secret_key_min_32_characters

# 암호화 키 생성: openssl rand -base64 32
ENCRYPTION_KEY=your_aes256_encryption_key

# =============================================================================
# AI APIs (선택 - 로컬에서는 없어도 앱 시작 가능)
# =============================================================================
OPENAI_API_KEY=sk-your_openai_api_key
GEMINI_API_KEY=your_gemini_api_key
```

**참고**: SQLite는 파일 기반 데이터베이스이므로 별도의 데이터베이스 서버가 필요하지 않습니다. 기본적으로 `./data/` 디렉토리에 데이터베이스 파일이 생성됩니다.

### 3. Build & Run

```bash
# 빌드
./gradlew build

# 로컬 프로파일로 실행
./gradlew bootRun --args='--spring.profiles.active=local'
```

### 4. Health Check

```bash
# API 헬스 체크
curl http://localhost:8080/api/v1/health
```

---

## 📚 Documentation

더 자세한 설계 및 요구사항 문서는 `docs/` 디렉토리를 참고해 주세요.

| 문서 | 설명 |
|------|------|
| `docs/GPT-SRS_v0.2.md` | 소프트웨어 요구사항 명세서 (SRS) |
| `docs/GPT-PRD.md` | 제품 요구사항 문서 (PRD) |
| `docs/WBS_DAG.md` | 작업 분해 구조 |

### 환경변수 참조

| 파일 | 설명 |
|------|------|
| `.env.example` | 환경변수 템플릿 (모든 키 목록) |
| `application.yml` | Spring 공통 설정 |
| `application-local.yml` | 로컬 개발 설정 |

---

## 🤝 Contribution

1. Issue를 생성하여 논의합니다.
2. Feature Branch(`feature/issue-number-name`)를 생성합니다.
3. 변경 사항을 커밋하고 Push합니다.
4. Pull Request를 생성합니다.

### 보안 관련 기여 시 주의사항

- **절대 `.env` 파일이나 실제 API 키를 커밋하지 마세요**
- PR 전 `git diff --cached`로 민감 정보 유출 여부를 확인하세요
- 새로운 환경변수 추가 시 `.env.example`에 템플릿을 함께 추가하세요

---

## 📄 License

Copyright © 2025 SCON Team. All Rights Reserved.
