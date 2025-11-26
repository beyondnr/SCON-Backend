# LawfulShift – 소상공인 직원 스케줄 관리 PRD v0.1

* Owner 팀: GPT Incubation · Compliance Automation Squad
* 최종 업데이트: 2025-11-14

## 1. 개요·목표

* **문제 정의 (Pain 지표 포함):**
  - 주휴·연장·야간 수당 계산 오류 자기보고율 67%(인터뷰 n=6)로 월평균 3.2건 정정, 1건당 과태료/체불 리스크 평균 ₩1.2M.
  - 직원 가용 시간 수집 채널이 카톡·전화·수기로 분산돼 승인 리드타임 48h, 결근/대타 공백률 18% (분기별 점검표).
  - 분쟁 시 증빙 부재율 60%(2/3 인터뷰)로 최근 12개월 내 합의금 지급 경험 1회 이상, 신뢰도 하락.
* **목표 (Desired Outcome 수치화):**
  - 법규 자동 계산과 위반 사전 경고로 오류 0건 상태를 유지하며 위반 사전 차단율 95% 이상.
  - 사장이 승인만 하는 3단계 온보딩으로 첫 스케줄 생성까지 10분 이내 70% 달성, 월말 정산 시간 90% 단축(5h→30m).
  - 증빙 자동 보관·내보내기로 분쟁 시 근거 제출 성공률 100%, 불안감 자체보고 지수 4.0→1.5.
* **성공 지표 (북극성/보조 KPI):**

| KPI | Baseline (Manual·인터뷰) | Target (MVP v1) | Measurement / Cadence |
| --- | --- | --- | --- |
| 법규 준수 완료율 (위반 없는 급여 사이클 비중) | 63% (월 3.2건 오류) | ≥98% | Compliance rule engine 로그, 월별 감사 리포트 |
| 온보딩 10분 내 완료율 | 18% (Excel 템플릿) | ≥70% | Product analytics funnel (가입→매장→스케줄) |
| 승인 리드타임 (요청→승인) | 48h 평균 | ≤6h, p90 ≤12h | 스케줄 이벤트 타임스탬프, 주간 |
| 증빙 내보내기 활용률 | 5% (수기/종이) | ≥50% | Audit log export 이벤트/급여 사이클, 월간 |
| 경고 대응률 (24h 내 해결) | 0% (사전 경고 부재) | ≥80% | Rule violation alert + Resolve status, 주간 |

## 2. 사용자와 페르소나

- 김영숙(58, 베이커리): 노동법 계산이 헷갈려 "불안해서 잠이 안 옴"; 스케줄·급여를 동시에 보여주는 단일 화면을 원함.
- 박철민(61, 국밥집): 알바생이 직접 스케줄 입력하고 본인은 승인만 하길 희망; 복잡 UI는 즉시 포기.
- 이정희(55, 꽃집): 세무사 경고 이후 '보험 같은' 자동 경고 시스템 요구; 규칙 업데이트에 민감.
- 윤지영(57, 의류): 과거 분쟁 경험으로 변경 이력·증빙 내보내기가 필수; 법정 분쟁 대비 신뢰 로그 우선.

## 3. 사용자 스토리와 수용 기준 (AC)

### Story 1 — 준법 급여 운영
As a risk-averse bakery owner, I want all allowances auto-calculated so that I can pay "법대로" without anxiety.

- AC1: Given 직원이 모바일 폼으로 주간 가용 시간을 제출하고 근로시간이 누적되었을 때, When 내가 '미리보기' 버튼을 누르면, Then 시스템이 주휴/연장/야간/휴일 수당을 p99 정확도 99.5% 이상으로 계산하고 차이를 색상으로 표시한다.
- AC2: Given 계산 결과가 주52시간, 휴게, 연속휴식 규칙 중 하나라도 위반할 경우, When 내가 승인 시도를 하면, Then 승인 버튼은 비활성화되고 위반 규칙 ID와 수정 가이드가 1초 내 표시된다.
- AC3: Given 모든 위반이 해소되어 승인하는 경우, When 내가 PDF/엑셀 증빙을 요청하면, Then 5초 내 다운로드 링크가 생성되고 AuditLog에 해시값이 저장되어 1년간 보관된다.
- AC4 (실패 케이스): Given 직원 입력이 누락되거나 중복된 경우, When 시스템이 모순을 감지하면, Then 알림(SMS/Kakao)으로 직원에게 30분 내 재입력을 요청하고 승인 플로우를 블록한다.

### Story 2 — 1클릭 스케줄 승인
As a low-tech restaurant owner, I want to approve consolidated shifts in one tap so that 운영 피로도를 줄인다.

- AC1: Given 최소 70% 직원이 모바일 링크를 통해 가용 시간을 제출했을 때, When 내가 사장 모드 대시보드를 열면, Then "승인 대기" 카드가 3단계(검토→조정→공지)로 표시되고 각 단계가 2초 내 로딩된다.
- AC2: Given 내가 스케줄 보드에서 드래그&드롭으로 교대 변경을 했을 때, When 저장을 누르면, Then 시스템이 임시 버전을 생성하고 15초 내 직원별 변경 알림을 단일 채널(Kakao/SMS)로 송신한다.
- AC3: Given 승인된 스케줄이 확정된 후, When 직원이 확인 여부를 미응답 상태로 12시간 이상 유지하면, Then 시스템이 재알림을 자동 발송하고 미확인률이 10% 초과 시 나에게 텍스트 알람을 보낸다.

## 4. 기능 요구사항 (Functional, MSCW)

| ID | MSCW | Requirement | 성공/근거 | 의존성 |
| --- | --- | --- | --- | --- |
| F-01 | Must | 3단계 온보딩(가입→매장 설정→첫 스케줄) 마법사 | 70% 10분 내 완료, Excel 대비 4.5배 빠름 | SMS 인증, 매장 템플릿 DB |
| F-02 | Must | 직원 모바일 가용시간 제출 폼 (QR/링크) | 참여율 ≥80%, 비로그인 제출 허용 | Short-link, 간편 본인확인 |
| F-03 | Must | 사장 1클릭 승인·공지 플로우 | 승인 리드타임 48h→6h, 대체 Excel 대비 8배 빠름 | Notification service |
| F-04 | Must | 노동법 준수 규칙 엔진 + 위반 경고 | 위반 사전 차단율 95% | 규칙 테이블, 노무사 검증 |
| F-05 | Must | 자동 급여/수당 계산 + 증빙 PDF/엑셀 | 오류율 ≤0.5%, 분쟁 증빙 제출 100% | 계산 모듈, 템플릿 엔진 |
| F-06 | Should | 변경 이력 Audit Log & 서명 추적 | 사건별 로그 100% 캡처, SHA256 서명 | Object storage |
| F-07 | Should | 간소 리포트 & 오프라인 출력 뷰 | 출력 요청 30초 내 생성, 점포 벽부착 대응 | Print-friendly CSS |
| F-08 | Could | 출퇴근 기록 기본 연동(수기 입력/사진) | 급여 정합성 ±2% 유지 | OCR/Photo upload |
| F-09 | Won't (v1) | POS/급여 솔루션 API 통합 | 후속 로드맵 | 외부 벤더 계약 필요 |

## 5. 비기능 요구사항 (NFR)

- **성능:** 스케줄 보드 조회 p95 800ms 이하, 승인 처리 API p95 1.2s 이하, PDF 생성 5s 이하(50직원 기준).
- **신뢰성:** 월 가용성 ≥99.5%, 규칙 엔진 오류율 ≤0.2% (자동 리트라이), 알림 전송 성공률 ≥97%.
- **보안/비용:** 모든 근로자 데이터 저장 시 AES-256, TLS1.3 강제; 국내 리전(VKR)만 사용; GDPR/근로기준법 기준 최소 수집(직원당 8개 필드 이하); 인프라 단위원가 ₩3,000/활성 매장 이하 유지.
- **모니터링:** Datadog 대시보드 `prd-lawfulshift-core`에서 API 지연, 규칙 엔진 실패, 알림 큐 적체를 실시간 추적; PagerDuty 경보 임계치 p95>1.2s 5분 지속 시 심야 알람.

## 6. 데이터·인터페이스 개요

- **핵심 엔터티**
  - Store: store_id, 업종, 영업시간, 대리인 정보.
  - Employee: employee_id, role, 계약유형, 시급, 연락처.
  - AvailabilitySubmission: submission_id, employee_id, 가능한 시간 슬롯, 타임스탬프, 제출 채널.
  - Schedule: schedule_id, store_id, 주차, 확정 상태, 승인자, 버전.
  - ShiftTemplate: template_id, 요일, 기본 규칙 ID.
  - ComplianceRule: rule_id, 법 조항, 계산식, 발효일, 심각도.
  - AuditLog: log_id, 엔터티, 액션, 이전/이후 값, 서명 해시.
- **API / 인터페이스**
  - `POST /api/v1/availability` (public, rate-limit 30/min): 입력 필드 = employee token, weekOf, slots[]; 유효성 실패 시 400과 원인코드 반환.
  - `GET /api/v1/schedules/{id}`: 포함 데이터 = shifts, violations[], approval_state; 캐싱 30s.
  - `POST /api/v1/schedules/{id}/approve`: Payload = adjustments[], notifyChannel; 규칙 위반 시 409 + rule_id.
  - `GET /api/v1/reports/payroll?weekOf=YYYY-WW`: 반환 = allowances[], PDF_link, export_hash.
  - Webhook `/events/violation`: 외부 파트너(노무사)에게 위반 발생 시 전송, 서명 헤더 포함.
  - Admin CSV import/export 인터페이스: labor rule 테이블 업데이트 (노무사만 접근).

## 7. 범위, 리스크·가정·의존성

- **In Scope (MVP v1):** 3단계 온보딩, 가용시간 제출 링크/QR, 사장 1클릭 승인, 규칙 엔진(주휴·연장·야간·휴일·주52시간·휴게·연속휴식), 증빙 PDF/엑셀, Audit Log v1, 단일 알림 채널(Kakao/SMS).
- **Out of Scope:** 멀티지점/역할 기반 권한, POS·급여 솔루션 API 연동, 다국어 UI, 고급 AI 배치 추천, 다채널 알림(Email Push) — 로드맵 Phase 1+.
- **리스크 & 대응**
  - R1 디지털 리터러시 부족 → 대리인 모드·전화 컨시어지·오프라인 출력 제공, 온보딩 가이드 영상.
  - R2 법령 변경 반영 지연 → 노무사 자문 계약(2주 SLA), 규칙 엔진 테이블 핫스왑 및 릴리즈 체크리스트.
  - R3 PII 유출/신뢰도 하락 → 필드 수준 암호화·접근통제(MFA), 분기별 침해대응 훈련.
  - R4 알림 채널 실패 → SMS 백업 라우팅, 발송 성공률 모니터링 임계치 95% 미만 시 자동 전환.
- **가정·의존성**
  - 고용노동부 공개 API 또는 정기 공지로 규칙 데이터 확보 가능(월 1회 업데이트).
  - Kakao/SMS 게이트웨이 SLA 99.9% 유지, 장애 시 4시간 내 복구.
  - 노무사 파트너가 규칙 변경 검토 메모를 48h 내 제공 (ADR-2025-02 참조).

## 8. 실험·롤아웃·측정

- **베타 채널:** Q4 디지털 취약 사업장 30곳(베이커리/국밥/꽃집) 대상 6주 베타. 롤아웃 단계 = 내부 dogfood → 컨시어지 온보딩 → Self-serve.
- **실험 가설:** "3단계 온보딩 + 1클릭 승인"이 월말 정산 시간 90% 단축, 위반 경고 처리율 80% 달성을 견인한다.
  - 디자인: A/B (컨시어지 vs 셀프), n=60 매장, 지표 = 온보딩 시간, 승인 리드타임, 오류율.
  - 성공 기준: p-value <0.05로 목표 상회, churn <10%.
- **경쟁 벤치마크:** Excel/수기 대비 계산 정확도 1.5배↑, 승인 속도 8배↑, 운영 비용 20%↓를 측정. 방법 = 동일 주차 데이터를 세 채널(Excel, 기존 SaaS, LawfulShift)로 처리, 처리시간+오류 수 비교.
- **롤아웃 가드레일:** 경고 대응률 60% 미만 or PDF 생성 실패 >2% 발생 시 롤아웃 중지, 핫픽스.

## 9. 근거 (Proof)

- `Analysis/6._SaaS_Employee_Schedule_Management_Service_Market_TAM-SAM-SOM+Market_Segment.md` — Q4 세그먼트 규모 및 디지털 역량 데이터.
- `Analysis/7._SaaS_Employee_Schedule_Management_Service_Market_Persona.md` — 페르소나 Pain/Needs, 자기보고 KPI 근거.
- `Analysis/9._SaaS_Employee_Schedule_Management_Service_JTBD_Interview_Summary_Card.md` — JTBD 진술과 원하는 Outcome, 오류/시간 지표.
- `Analysis/8._SaaS_Employee_Schedule_Management_Service_Market_AOS-DOS_Matrix_and_Comprehensive_Evaluation_Table.md` — AOS/DOS 4.0 측정값과 차별 가치.
- 고용노동부 2024 상반기 임금체불 통계 (임금체불액 1조 원) — 규제 위험 근거.

