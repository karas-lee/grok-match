# LogCenter Format Recommender

SIEM(Security Information and Event Management) 시스템을 위한 고성능 로그 포맷 추천 엔진입니다. Grok 패턴을 사용하여 다양한 로그 포맷을 자동으로 식별하고 추천합니다.

🚀 **특징**: 실시간 분석, 병렬 처리, API 클라이언트, 캐싱 지원

## 주요 기능

- **로그 포맷 자동 인식**: 458개의 사전 정의된 로그 포맷 지원
- **Grok 패턴 매칭**: 표준 및 커스텀 Grok 패턴을 사용한 정확한 로그 파싱
- **다중 포맷 추천**: 신뢰도 점수 기반 상위 N개 포맷 추천
- **벤더별/그룹별 필터링**: 특정 벤더나 그룹의 로그 포맷만 검색
- **다양한 출력 형식**: TEXT, JSON, CSV 형식 지원
- **병렬 처리**: 대용량 로그 파일의 빠른 처리

## 지원 로그 포맷

- **보안 장비**: Firewall, IPS, WAF, UTM 등
- **네트워크 장비**: Router, Switch, Load Balancer 등
- **서버/OS**: Linux, Windows, AIX 등
- **애플리케이션**: Web Server, Database, Middleware 등
- **클라우드**: AWS, Azure, GCP 등

총 102개 로그 포맷, 10개 그룹, 52개 벤더 지원

## 프로젝트 현황

- **버전**: 1.0.0
- **개발 진행률**: 85% (Phase 8/10 완료)
- **테스트 커버리지**: 98%
- **패턴 성공률**: 91.5% (419/458)

## 설치 및 빌드

### 요구사항
- Java 8 이상
- Maven 3.6 이상

### 빌드
```bash
mvn clean package
```

### 실행 가능한 JAR 생성
```bash
mvn clean package

# 단독 실행 가능한 JAR
java -jar target/logcenter-format-recommender-1.0.0-jar-with-dependencies.jar
```

## 사용법

### 기본 사용법
```bash
# 단일 로그 라인 분석
java -jar logcenter-format-recommender.jar "로그 내용"

# 파일 분석
java -jar logcenter-format-recommender.jar -f access.log

# 디렉토리 분석
java -jar logcenter-format-recommender.jar -d /var/log
```

### 옵션
- `-f, --file`: 로그 파일 경로
- `-d, --directory`: 로그 디렉토리 경로
- `-g, --group`: 특정 그룹으로 필터링 (FIREWALL, WEBSERVER 등)
- `-v, --vendor`: 특정 벤더로 필터링
- `-t, --top`: 상위 N개 결과만 표시 (기본값: 10)
- `--format`: 출력 형식 (TEXT, JSON, CSV)
- `--verbose`: 상세 정보 출력
- `--list-formats`: 지원하는 모든 로그 포맷 목록 표시
- `--list-groups`: 지원하는 그룹 목록 표시
- `--list-vendors`: 지원하는 벤더 목록 표시

### 기본 옵션

| 옵션 | 설명 | 기본값 |
|------|------|--------|
| `-f, --file` | 로그 파일 경로 | - |
| `-d, --directory` | 로그 디렉토리 경로 | - |
| `-g, --group` | 특정 그룹으로 필터링 | - |
| `-v, --vendor` | 특정 벤더로 필터링 | - |
| `-n, --top` | 상위 N개 결과만 표시 | 5 |
| `-m, --min-confidence` | 최소 신뢰도 임계값 | 70 |
| `-o, --output` | 출력 형식 (text/json/csv) | text |
| `--detail` | 상세 정보 표시 | false |
| `--stats` | 통계 정보 표시 | false |
| `--list-formats` | 지원하는 모든 로그 포맷 목록 | - |
| `--list-groups` | 지원하는 그룹 목록 | - |
| `--list-vendors` | 지원하는 벤더 목록 | - |

### API 클라이언트 옵션

| 옵션 | 설명 | 기본값 |
|------|------|--------|
| `--api` | API 서버를 통한 추천 | false |
| `--api-url` | API 서버 URL | api.properties |
| `--api-key` | API 인증 키 | api.properties |

### 예제

```bash
# Apache 액세스 로그 분석
java -jar logcenter-format-recommender.jar -f /var/log/apache2/access.log

# Firewall 그룹의 로그만 검색
java -jar logcenter-format-recommender.jar -f security.log -g FIREWALL

# JSON 형식으로 상위 5개 결과 출력
java -jar logcenter-format-recommender.jar -f app.log -t 5 --format JSON

# 지원하는 모든 포맷 확인
java -jar logcenter-format-recommender.jar --list-formats

# 특정 벤더의 로그 포맷만 확인
java -jar logcenter-format-recommender.jar --list-formats -v "CISCO SYSTEMS"

# 통계 정보와 함께 상세 결과 출력
java -jar logcenter-format-recommender.jar -f server.log --detail --stats

# 최소 신뢰도 80% 이상인 결과만 표시
java -jar logcenter-format-recommender.jar -f app.log -m 80

# API 모드로 실행
java -jar logcenter-format-recommender.jar --api --api-url http://api.example.com "로그 샘플"
```

## API 클라이언트 사용법

### 환경변수 설정
```bash
export LOGCENTER_API_URL=http://api.example.com
export LOGCENTER_API_KEY=your-api-key
java -jar logcenter-format-recommender.jar --api "로그 샘플"
```

### 설정 파일 (api.properties)
```properties
# API 서버 URL
api.url=http://localhost:8080

# API 인증 키 (선택사항)
api.key=your-api-key

# API 사용 여부 (true/false)
api.enabled=false

# 캐시 사용 여부
api.cache.enabled=true
```

## 개발자 가이드

### 프로젝트 구조
```
src/main/java/com/logcenter/recommender/
├── Main.java                    # 진입점
├── cli/                         # CLI 인터페이스
│   ├── CliCommand.java         # Picocli 명령어 정의
│   └── OutputFormatter.java    # 출력 포맷 처리
├── config/                      # 설정 관리
├── grok/                        # Grok 패턴 처리
│   ├── GrokCompilerWrapper.java
│   ├── CustomPatternLoader.java
│   └── PatternNormalizer.java
├── matcher/                     # 로그 매칭 엔진
│   ├── LogMatcher.java
│   └── AdvancedLogMatcher.java
├── model/                       # 데이터 모델
├── repository/                  # 패턴 저장소
├── service/                     # 비즈니스 로직
└── util/                        # 유틸리티
```

### 커스텀 패턴 추가
1. `resources/custom-grok-patterns` 파일에 패턴 추가
2. `resources/setting_logformat.json`에 로그 포맷 정의 추가

### 테스트
```bash
# 전체 테스트
mvn test

# 패턴 테스트
mvn exec:java -Dexec.mainClass="com.logcenter.recommender.test.PatternTestRunner"

# 특정 패턴 상세 테스트
mvn exec:java -Dexec.mainClass="com.logcenter.recommender.test.DetailedPatternTester"
```

## 매칭 신뢰도 계산 방식

### 신뢰도 점수 체계
로그 포맷 매칭의 신뢰도는 0-100% 범위로 계산되며, 다음 요소들을 종합적으로 평가합니다:

#### 1. 완전 매칭 (Complete Match)
- **단일 완전 매칭**: 98% 고정 신뢰도
- **다중 완전 매칭**: 필드 수와 구체성에 따라 차등 적용

#### 2. 필드 수 기반 평가
log_time과 message 필드를 제외한 유효 필드 수에 따른 신뢰도 조정:

| 유효 필드 수 | 최대 신뢰도 | 설명 |
|------------|-----------|------|
| 0-2개 | 70% | 의미있는 정보 부족으로 강한 페널티 |
| 3개 | 80% | 기본적인 정보만 포함 |
| 4-5개 | 90% | 적절한 수준의 정보 포함 |
| 6개 이상 | 98% | 충분한 정보로 높은 신뢰도 |

#### 3. 필드 구체성 평가
구체적인 필드(src_ip, dst_ip, protocol 등)와 일반적인 필드(message, data 등)의 비율:

- **매우 구체적** (3개 이상): +4-6% 보너스
- **구체적** (2개): +2-4% 보너스  
- **약간 구체적** (1개): +1-2% 보너스
- **일반적**: 보너스 없음

#### 4. 필드 의미 검증
필드명과 값의 의미적 일치를 검증하여 잘못된 매칭 필터링:

- **IP 필드 검증**: IPv4 형식, 특수값(-, ?.?.?.?)
- **포트 필드 검증**: 0-65535 범위
- **타임스탬프 검증**: 다양한 날짜/시간 형식
- **프로토콜 검증**: tcp, udp, icmp 등 유효한 프로토콜
- **액션 필드 검증**: allow, deny, drop 등 보안 액션
- **디바이스명 검증**: 영문자, 숫자, 하이픈 조합

의미적으로 맞지 않는 필드가 3개 이상이면 매칭 거부

#### 5. 그룹별 가중치
로그 포맷 그룹에 따른 신뢰도 조정:

| 그룹 | 가중치 | 설명 |
|------|--------|------|
| FIREWALL | 1.2x | 방화벽 로그 우선순위 높음 |
| IPS | 1.2x | 침입방지시스템 중요도 높음 |
| WAF | 1.1x | 웹방화벽 중요도 상승 |
| WEBSERVER | 1.0x | 웹서버 표준 가중치 |
| SYSTEM | 0.9x | 시스템 로그 일반 가중치 |
| APPLICATION | 0.8x | 애플리케이션 로그 낮은 가중치 |

### 신뢰도 계산 예시

#### 예시 1: 방화벽 로그 (높은 신뢰도)
```
로그: "192.168.1.100,80 -> 10.0.0.1,443 tcp allow"
매칭 필드: src_ip, src_port, dst_ip, dst_port, protocol, action (6개)
구체적 필드: 6개 모두 구체적
그룹: FIREWALL
최종 신뢰도: 98% × 1.2 = 98% (최대값 제한)
```

#### 예시 2: 일반 로그 (낮은 신뢰도)
```
로그: "2024-01-01 Error occurred"
매칭 필드: log_time, message (실질적으로 0개)
구체적 필드: 0개
최종 신뢰도: 50% (최소값)
```

### 부분 매칭 (Partial Match)
완전 매칭이 실패한 경우, 다음 기준으로 부분 매칭 점수 계산:
- 필드 수 점수: 40%
- 구체적 필드 점수: 20%
- 커버리지 점수: 20%
- 검증된 필드 점수: 10%
- 필수 필드 점수: 10%

부분 매칭은 최대 70%로 제한됩니다.

## 성능

### 처리 속도
- 단일 로그: < 100ms
- 1,000개 로그: < 1초
- 10,000개 로그: < 5초 (병렬 처리 시)

### 메모리 사용량
- 초기 로드: ~100MB
- 실행 중: ~200-300MB
- 최대: ~500MB (대용량 처리 시)

### 패턴 성능
- 전체 패턴: 458개
- 성공률: 91.5% (419/458)
- 평균 매칭 시간: 10ms/패턴

## 고급 기능

### 패턴 필터링
PatternFilter 클래스를 통해 너무 일반적인 패턴을 자동으로 거릅합니다:
- `^%{LOG_TIME:log_time} %{MESSAGE:message}$` 같은 단순 패턴 제외
- MESSAGE/GREEDYDATA가 주요 필드인 패턴 필터링
- 필드 수가 2개 미만인 패턴 제외

### 병렬 처리
- CPU 코어 수에 따라 자동 스레드 풀 크기 조절
- 각 로그에 대해 독립적인 매칭 수행
- 타임아웃 설정으로 무한 루프 방지

### 캐싱
- Caffeine 기반 고성능 캐시
- TTL 기반 자동 만료
- 멀티 레벨 캐시 (logFormats, recommendations, apiResponses)

## 신뢰도 계산 방식

본 시스템은 다음과 같은 개선된 알고리즘으로 로그 포맷의 신뢰도를 계산합니다:

### 기본 신뢰도 계산

1. **단일 완전 매칭 (Single Complete Match)**: 88-98%
   - Grok 패턴이 로그 전체를 완벽하게 매칭
   - 필드 품질에 따라 차등 적용:
     - 매우 구체적인 필드 (3개 이상): 96-98%
     - 구체적인 필드 (2개): 93-96%
     - 약간의 구체적인 필드 (1개): 90-93%
     - 일반적인 필드만: 88-90%

2. **다중 완전 매칭 (Multiple Complete Match)**: 90-98%
   - 여러 포맷이 동시에 매칭되는 경우
   - 필드 수와 구체성에 따라 차등 적용

3. **부분 매칭 (Partial Match)**: 최대 70%
   - 일부만 매칭되는 경우
   - 신뢰도 = matchScore × 70%

4. **매칭 실패**: 0%

### 매칭 점수 (Match Score) 계산

매칭 점수는 다음 요소들의 가중 평균으로 계산됩니다:

```
matchScore = (fieldScore × 0.4) + (specificFieldScore × 0.2) + 
             (coverageScore × 0.2) + (validationScore × 0.1) + 
             (requiredFieldScore × 0.1)
```

#### 1. 필드 점수 (Field Score) - 40% ⬆️
- 추출된 필드 수에 따른 점수 (log_time, message 제외)
- 많은 필드를 추출할수록 높은 점수
- 기존 30%에서 40%로 가중치 증가

#### 2. 구체적 필드 점수 (Specific Field Score) - 20% 🆕
- 새로 추가된 평가 요소
- 구체적인 필드(src_ip, dst_ip, protocol 등)의 존재 여부
- 일반적인 필드(message, data)보다 높은 점수

#### 3. 커버리지 점수 (Coverage Score) - 20% ⬇️
- 매칭된 텍스트 길이 / 전체 로그 길이
- 기존 30%에서 20%로 가중치 감소

#### 4. 검증 점수 (Validation Score) - 10% ⬇️
- 필드 타입 검증 (IP, 포트, 타임스탬프 등)
- 기존 20%에서 10%로 가중치 감소

#### 5. 필수 필드 점수 (Required Field Score) - 10%
- 필수 필드 추출 비율
- 변경 없음

### 완전 매칭 기준 강화

#### 최소 필드 수 요구사항
- 기본: 3개 초과 필드 필요 (log_time, message 제외)
- 예외: 구체적인 필드가 2개 이상인 경우 허용

#### 구체적인 필드 목록
```
- 네트워크: src_ip, dst_ip, src_port, dst_port
- 보안: protocol, action, rule_id, attack_id
- 시스템: user_id, session_id, event_id
- 방향: src, dst, source, destination
```

### 그룹 가중치

특정 로그 그룹은 추가 가중치를 받습니다:

- **FIREWALL**: 1.2x
- **IPS**: 1.2x
- **WAF**: 1.1x
- **WEBSERVER**: 1.0x
- **SYSTEM**: 0.9x
- **APPLICATION**: 0.8x
- 기타: 1.0x

### 다중 매칭 차등 신뢰도

여러 포맷이 동시에 완전 매칭될 경우:
- 기본 신뢰도: 90%
- 필드 수 가산점: 최대 5점 (유효 필드 수 × 0.5)
  - log_time, message, msg, raw_message 필드는 제외
- 구체적 필드 가산점: 최대 3점 (구체적 필드 수 × 1.0)
- 최대 신뢰도: 98%

### 예시

```
로그: "192.168.1.100:8080 -> 10.0.0.1:443"
패턴: "%{IP:src_ip}:%{INT:src_port} -> %{IP:dst_ip}:%{INT:dst_port}"

1. 완전 매칭 → 기본 98%
2. 필드 점수: 4/4 = 100%
3. 커버리지: 100%
4. 검증: 모든 IP와 포트 유효 = 100%
5. FIREWALL 그룹 → 1.2x 가중치
6. 최종 신뢰도: 98% (상한선 적용)
```

## 알려진 이슈

1. **패턴 컴파일 오류**: 39개 패턴(8.5%)에서 컴파일 오류. 자세한 내용은 [pattern_error.md](pattern_error.md) 참조.
2. **너무 일반적인 패턴**: 일부 포맷이 너무 일반적인 패턴을 사용하여 잘못된 매칭 발생 가능

## 라이선스

이 프로젝트는 내부 사용 목적으로 개발되었습니다.

## 기여

버그 리포트나 기능 제안은 이슈 트래커를 통해 제출해 주세요.

## 버전 히스토리

### v1.0.2 (2025-08-06)
- 🔧 로그 포맷 검증 도구 추가
  - 배포 전 모든 포맷 검증 기능
  - 패턴 컴파일 오류 검출
  - 샘플 로그 매칭 테스트
  - 다양한 리포트 형식 (TEXT, HTML, JSON)
  - CI/CD 파이프라인 통합 지원

### v1.0.1 (2025-08-05)
- 🔧 신뢰도 계산 개선
  - 필드 수 계산 시 log_time, message 필드 제외
  - 구체적인 필드(src_ip, dst_ip 등) 가중치 증가
  - 필드 품질에 따른 차등 신뢰도 적용 (88-98%)
  - 정렬 우선순위 개선: 구체적 필드 > 유효 필드 수 > 신뢰도

### v1.0.0 (2025-08-05)
- 🎆 초기 릴리즈
- ✅ 458개 로그 포맷 지원 (102개 포맷, 10개 그룹, 52개 벤더)
- ✅ CLI 인터페이스 구현 (Picocli)
- ✅ 병렬 처리 지원
- ✅ API 클라이언트 구현
- ✅ 패턴 필터링 기능
- ✅ 다양한 출력 형식 (TEXT, JSON, CSV)

## 로그 포맷 검증 도구

### 개요
배포 전 모든 로그 포맷의 유효성을 검증하는 도구입니다.

### 빠른 시작
```bash
# 기본 검증 실행
java -cp target/logcenter-format-recommender-1.0.0-jar-with-dependencies.jar \
  com.logcenter.recommender.validator.ValidatorMain

# HTML 리포트 생성
java -cp target/logcenter-format-recommender-1.0.0-jar-with-dependencies.jar \
  com.logcenter.recommender.validator.ValidatorMain \
  --format html -o validation-report.html
```

### 검증 항목
- **패턴 컴파일**: Grok 패턴 문법 오류 검출
- **샘플 매칭**: 샘플 로그와 패턴 매칭 테스트
- **품질 검사**: 필드 수, GREEDYDATA 사용 등 경고
- **성능 검증**: 타임아웃 및 처리 시간 측정

### 출력 형식
- **TEXT**: 콘솔 출력용 텍스트 형식 (기본)
- **HTML**: 웹 브라우저에서 볼 수 있는 상세 리포트
- **JSON**: CI/CD 파이프라인 통합용

📖 자세한 사용법은 [로그 포맷 검증 도구 가이드](docs/LOG_FORMAT_VALIDATOR_GUIDE.md)를 참조하세요.

## 향후 계획

- [ ] E2E 테스트 추가
- [ ] 더 엄격한 패턴 필터링
- [ ] 웹 UI 개발
- [ ] Docker 이미지 제공
- [ ] Kubernetes Helm 차트
- [ ] 검증 도구 성능 최적화

## 기여 방법

1. Fork & Clone
2. 기능 개발
3. 테스트 작성
4. Pull Request 제출

### 코드 스타일
- Java 8 호환
- 4 스페이스 들여쓰기
- JavaDoc 필수
- 테스트 커버리지 80% 이상