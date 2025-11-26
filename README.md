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
- **Language**: Java 17 (LTS)
- **Framework**: Spring Boot 3.x
- **Build Tool**: Gradle
- **Database**: MySQL 8.x (InnoDB)
- **ORM**: Spring Data JPA (Hibernate)

### Architecture & Integration
- **Architecture**: Monolithic Service (MVP Phase)
- **API Style**: RESTful API (JSON)
- **Document Gen**: Python + FastAPI + LangChain (Microservice)
- **External**: KakaoTalk/SMS Gateway, OpenAI/Gemini (LLM for future expansion)

## 📂 Project Structure

```text
src/main/java/vibe/scon/scon_backend
├── config          # Spring Configuration
├── controller      # REST Controllers
├── service         # Business Logic
├── repository      # Data Access Layer (JPA)
├── entity          # JPA Entities (Domain)
├── dto             # Data Transfer Objects
├── exception       # Global Exception Handling
└── util            # Utility Classes
```

## ⚡ Getting Started

### Prerequisites
- JDK 17 이상
- MySQL 8.x
- Git

### Installation & Running

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd SCON-Backend
   ```

2. **Database Setup**
   - MySQL 데이터베이스를 생성합니다 (`scon_db` 등).
   - `src/main/resources/application.properties` (또는 `application.yml`) 파일에서 DB 접속 정보를 수정합니다.

3. **Build**
   ```bash
   # Windows
   ./gradlew build

   # macOS/Linux
   ./gradlew build
   ```

4. **Run**
   ```bash
   # Windows
   ./gradlew bootRun

   # macOS/Linux
   ./gradlew bootRun
   ```

## 📚 Documentation
더 자세한 설계 및 요구사항 문서는 `docs/` 디렉토리를 참고해 주세요.
- `docs/GPT-SRS_v0.2.md`: 소프트웨어 요구사항 명세서 (SRS)
- `docs/GPT-PRD.md`: 제품 요구사항 문서 (PRD)

## 🤝 Contribution
1. Issue를 생성하여 논의합니다.
2. Feature Branch(`feature/issue-number-name`)를 생성합니다.
3. 변경 사항을 커밋하고 Push합니다.
4. Pull Request를 생성합니다.

---
Copyright © 2025 SCON Team. All Rights Reserved.

