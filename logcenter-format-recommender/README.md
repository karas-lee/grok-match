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
- **개발 진행률**: 80% (Phase 7/10 완료)
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

## 알려진 이슈

1. **패턴 컴파일 오류**: 39개 패턴(8.5%)에서 컴파일 오류. 자세한 내용은 [pattern_error.md](pattern_error.md) 참조.
2. **너무 일반적인 패턴**: 일부 포맷이 너무 일반적인 패턴을 사용하여 잘못된 매칭 발생 가능

## 라이선스

이 프로젝트는 내부 사용 목적으로 개발되었습니다.

## 기여

버그 리포트나 기능 제안은 이슈 트래커를 통해 제출해 주세요.

## 버전 히스토리

### v1.0.0 (2025-08-05)
- 🎆 초기 릴리즈
- ✅ 458개 로그 포맷 지원 (102개 포맷, 10개 그룹, 52개 벤더)
- ✅ CLI 인터페이스 구현 (Picocli)
- ✅ 병렬 처리 지원
- ✅ API 클라이언트 구현
- ✅ 패턴 필터링 기능
- ✅ 다양한 출력 형식 (TEXT, JSON, CSV)

## 향후 계획

- [ ] E2E 테스트 추가
- [ ] 더 엄격한 패턴 필터링
- [ ] 웹 UI 개발
- [ ] Docker 이미지 제공
- [ ] Kubernetes Helm 차트

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