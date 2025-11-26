# REQ-NF-OPS: 모니터링 및 운영 효율화

## 1. 목적 및 요약
* **목적**: 서비스 상태를 실시간으로 파악하고, 이상 징후를 조기에 발견하여 대응하며, 운영 비용을 최적화한다.
* **요약**: 로깅 표준화, 메트릭 수집(Actuator/CloudWatch), 알림 임계값 설정, 그리고 비용 모니터링 체계를 구축한다.

## 2. 관련 REQ
* `REQ-NF-012`: 매장당 월 운영 비용 (3,000원 이하)
* `REQ-NF-013`: 실시간 모니터링 범위
* `REQ-NF-014`: 알림 임계값 (Latency > 1.2s 지속 시 Alert)
* `REQ-NF-016`, `017`: 유지보수성 (리드타임, 핫픽스)

## 3. 요구사항 상세
* **로깅**: JSON 포맷 로그(Logstash/Logback) 적용, TraceId(MDC) 전파.
* **메트릭**: 주요 API 응답시간, 에러율, JVM 힙 메모리 등 수집.
* **알람**: 장애 등급(Critical, Warning) 분류 및 전송 채널 설정.
* **비용**: 리소스 사용량 최적화 (t4g 인스턴스 활용 등).

## 4. 구현 및 검증 활동
1. **Logback Config**: JSON Appender 설정, 비즈니스 키워드 태깅.
2. **Monitoring Setup**: CloudWatch Agent 또는 Prometheus/Grafana 연동.
3. **Dashboard**: 핵심 지표(KPI)를 한눈에 보는 대시보드 구성.

---

```yaml
task_id: "REQ-NF-OPS"
title: "모니터링, 로깅 및 운영 체계 구축"
summary: >
  시스템 관측성(Observability) 확보를 위한 로깅/모니터링 설정과
  운영 비용 및 유지보수 효율화를 위한 기반을 마련한다.
type: "non_functional"

epic: "E9_NFR"
req_ids: ["REQ-NF-012", "REQ-NF-013", "REQ-NF-014"]
component: ["backend.ops", "infra"]

category: "observability"
labels:
  - "observability:logging"
  - "observability:monitoring"
  - "operations:cost"

context:
  srs_section: "4.2 Non-Functional Requirements"
  related_req_func: []

requirements:
  description: >
    안정적 운영을 위한 가시성 확보.
  kpis:
    - "로그 인덱싱 지연 < 1분"
    - "장애 발생 시 5분 내 알림 발송"

implementation_scope:
  includes:
    - "Logback JSON Encoder 설정"
    - "Spring Boot Actuator Metric Export"
    - "CloudWatch/Alert Manager 설정 가이드"

steps_hint:
  - "요청별 TraceId 생성(Filter) 및 로그 포함."
  - "비용 효율적인 인스턴스 타입 선정 및 Auto Scaling 정책 검토."
  - "CI/CD 파이프라인에 빌드/배포 시간 측정 단계 추가 (리드타임 관리)."

preconditions:
  - "배포 환경이 결정되어야 함."

postconditions:
  - "대시보드에서 실시간 트래픽과 에러를 볼 수 있어야 함."

tests:
  - "인위적 에러 발생 후 알림 수신 테스트"

dependencies: ["TASK-INFRA-SETUP"]

parallelizable: true
estimated_effort: "S"
priority: "Should"
agent_profile: ["devops"]

required_tools:
  languages: ["YAML", "XML"]
  infra: ["AWS CloudWatch", "Grafana"]

references:
  srs: ["REQ-NF-013", "REQ-NF-014"]

risk_notes:
  - "로그 양이 많으면 비용이 증가하므로 적절한 Log Level(INFO/WARN) 조정 필요."

```

