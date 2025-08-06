# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

**중요**: 모든 문서 및 답변은 한글로 작성 및 답변을 합니다.

## 프로젝트 개요

LogCenter Format Recommendation System (LFRS) - Grok 패턴 매칭을 사용하여 100개 이상의 사전 정의된 포맷 중에서 최적의 로그 포맷을 자동으로 식별하는 SIEM 로그 포맷 추천 시스템입니다.

## 핵심 아키텍처

### 코어 매칭 알고리즘
시스템은 Grok 완전 매칭을 우선시합니다:
- **단일 완전 매칭**: 98% 신뢰도로 즉시 반환
- **다중 완전 매칭**: 필드 수, GREEDYDATA 사용, 패턴 구체성을 기반으로 평가 (90-98%)
- **부분 매칭만 존재**: 최대 70% 신뢰도로 제한적 평가
- **매칭 없음**: 빈 결과 반환

### 신뢰도 계산 핵심 로직
- `log_time`, `message` 필드는 신뢰도 계산에서 제외
- 구체적 필드(`src_ip`, `dst_ip`, `protocol`, `action` 등) 가중치 적용
- 그룹별 가중치: FIREWALL(1.2), IPS(1.2), WAF(1.1)
- GREEDYDATA 패턴 과다 사용 시 감점

### 핵심 데이터 파일
1. **`docs/custom-grok-patterns`**: 233개의 커스텀 Grok 패턴 (`src/main/resources/`로 복사 필요)
   - TEXT 패턴 (TEXT1-TEXT18)
   - IP/Port 패턴 (SRC_IP, DST_IP 등)
   - Date/Time 패턴 (DATE_FORMAT1-10)
   - 특수 패턴 (CISCO, SKIP, EMAIL 등)

2. **`docs/GROK-PATTERN-CONVERTER.sql`** 또는 **`setting_logformat.json`**: 100개의 로그 포맷 정의
   - 그룹: Firewall (23), Web Server (4), System (11), WAF (14), IPS (8), Application (18) 등

## 빌드 및 개발 명령어

### Maven 빌드
```bash
# 테스트 포함 클린 빌드
mvn clean install

# 테스트 없이 빌드
mvn clean package -DskipTests

# 단위 테스트 실행
mvn test
mvn test -Dtest=AdvancedLogMatcherTest  # 특정 테스트 실행

# 통합 테스트 실행
mvn verify

# 특정 프로파일로 빌드
mvn clean package -Pdev   # 개발 환경 (log.level=DEBUG)
mvn clean package -Pprod  # 운영 환경 (log.level=INFO)
```

### 애플리케이션 실행
```bash
# 단일 로그 분석
java -jar target/logcenter-format-recommender-1.0.0-jar-with-dependencies.jar "로그 샘플"

# 파일/디렉토리 분석
java -jar target/logcenter-format-recommender-1.0.0-jar-with-dependencies.jar -f /path/to/logfile.log
java -jar target/logcenter-format-recommender-1.0.0-jar-with-dependencies.jar -d /log/directory

# 필터링 옵션
java -jar target/logcenter-format-recommender-1.0.0-jar-with-dependencies.jar --group FIREWALL --vendor CISCO "로그"
java -jar target/logcenter-format-recommender-1.0.0-jar-with-dependencies.jar --min-confidence 80 --top 10 "로그"

# 캐시 관리
java -jar target/logcenter-format-recommender-1.0.0-jar-with-dependencies.jar --clear-cache
java -jar target/logcenter-format-recommender-1.0.0-jar-with-dependencies.jar --rebuild-cache
java -jar target/logcenter-format-recommender-1.0.0-jar-with-dependencies.jar --no-cache "로그"
java -jar target/logcenter-format-recommender-1.0.0-jar-with-dependencies.jar --cache-dir /custom/cache "로그"

# API 모드
java -jar target/logcenter-format-recommender-1.0.0-jar-with-dependencies.jar --api --api-url http://server:8080 "로그"

# 메모리 설정과 함께 실행
java -Xmx2G -Xms512M -jar target/logcenter-format-recommender-1.0.0-jar-with-dependencies.jar "로그"
```

### 개발 유틸리티
```bash
# 의존성 트리 확인
mvn dependency:tree

# JavaDoc 생성
mvn javadoc:javadoc

# 프로젝트 effective POM 확인
mvn help:effective-pom

# 의존성 분석
mvn dependency:analyze
```

## 최근 변경사항

### PersistentCacheManager 구현 완료 (2025-08-06)
- **영구 캐시 시스템 구현**: `com.logcenter.recommender.cache.PersistentCacheManager`
  - 컴파일된 Grok 패턴과 로그 포맷을 디스크에 캐싱
  - TTL 기반 캐시 만료 관리
  - SHA-256 체크섬 기반 캐시 유효성 검증
  - 캐시 디렉토리: `~/.logcenter/cache/` (기본값)

## 기술적 제약사항

- **JDK**: 반드시 1.8 호환
- **Grok 라이브러리**: io.krakens:java-grok:0.1.9 (JDK 1.8 호환)
- **JSON 처리**: JDK 1.8 호환성을 위해 Gson 사용 (Jackson도 일부 사용)
- **리소스 로딩**: JAR 호환성을 위해 `ClassLoader.getResourceAsStream()` 사용
- **메모리**: 최소 512MB, 운영 환경 2GB 권장

## 프로젝트 구조 및 핵심 컴포넌트

```
logcenter-format-recommender/
├── pom.xml                    # Maven 설정 (JDK 1.8, Assembly Plugin)
├── src/main/java/com/logcenter/recommender/
│   ├── Main.java              # 진입점
│   ├── api/                   # REST API 클라이언트
│   │   ├── cache/            # API 캐시 관리
│   │   └── client/           # HTTP 클라이언트
│   ├── cache/                 # 영구 캐시 시스템
│   │   └── PersistentCacheManager.java
│   ├── cli/                   # CLI 인터페이스 (Picocli)
│   │   └── CliCommand.java   # 명령행 처리
│   ├── config/                # 다층 설정 로딩
│   │   ├── ApiConfiguration.java
│   │   └── AppConfig.java
│   ├── filter/                # 패턴 필터링
│   ├── grok/                  # Grok 엔진 래핑
│   │   ├── CachedGrokCompilerWrapper.java
│   │   ├── CustomPatternLoader.java
│   │   └── FilePatternRepository.java
│   ├── matcher/               # 매칭 엔진
│   │   ├── AdvancedLogMatcher.java
│   │   └── SimpleLogMatcher.java
│   ├── model/                 # 데이터 모델
│   ├── service/               # 비즈니스 로직
│   │   └── LogFormatRecommenderImpl.java
│   └── util/                  # 유틸리티
└── src/main/resources/
    ├── application.properties # 애플리케이션 설정
    ├── api.properties        # API 클라이언트 설정
    ├── logback.xml          # 로깅 설정
    ├── custom-grok-patterns # 233개 커스텀 패턴
    └── setting_logformat.json # 100개 로그 포맷
```

## 중요 구현 참고사항

### 패턴 처리
1. **커스텀 패턴 로딩**: 표준 패턴보다 먼저 `custom-grok-patterns` 파일에서 233개 패턴을 모두 로드
2. **패턴 우선순위**: 이름 충돌 시 커스텀 패턴이 표준 패턴보다 우선
3. **패턴 컴파일 캐싱**: `CachedGrokCompilerWrapper`로 재컴파일 방지

### 성능 최적화
1. **병렬 처리**: `ExecutorService`와 `CompletableFuture` 사용
2. **다층 캐싱**: Caffeine 메모리 캐시 + 영구 캐시 (구현 완료)
3. **타임아웃**: 매칭 30초, API 호출 30초
4. **영구 캐시**: 첫 실행 2-3초 → 이후 0.5초 이하

### 설정 로딩 우선순위
1. 시스템 프로퍼티 (`-Dapp.*`)
2. 지정 설정 파일 (`-Dconfig.file`)
3. 지정 디렉토리 (`-Dconfig.dir`)
4. 현재 디렉토리
5. 사용자 홈 (`~/.logcenter/`)
6. 클래스패스 (기본값)

## 테스트 구조

```
src/test/java/com/logcenter/recommender/
├── api/cache/CacheManagerTest.java
├── cli/CliCommandTest.java
├── config/                    # 설정 테스트
├── filter/PatternFilterTest.java
├── grok/                      # Grok 엔진 테스트
├── integration/               # 통합 테스트 (**/integration/*Test.java)
├── matcher/                   # 매칭 엔진 테스트
│   ├── AdvancedLogMatcherTest.java
│   └── SimpleLogMatcherTest.java
├── model/                     # 모델 테스트
├── service/LogFormatRecommenderImplTest.java
└── util/                      # 유틸리티 테스트
```

### 테스트 리소스
- `test-logs/firewall-sample.log`
- `test-logs/system-sample.log`
- `test-logs/webserver-sample.log`

## Python 유틸리티

`script/grok-converter.py`: LOGCENTER-LOG-FORMAT.sql (1.2MB)에서 `data_table` 항목을 제거하여 GROK-PATTERN-CONVERTER.sql (366KB) 생성

## 주요 문서

- **PRD 문서**: `docs/LOGCENTER-LOG-FORMAT-RECOMMEND-PRD.md` - 상세 제품 요구사항
- **진행률 추적**: `task/PROGRESS_FORMAT_RECOMMEND.md` - 개발 체크리스트 (현재 90% 완료)
- **커스텀 패턴**: `docs/custom-grok-patterns` - 233개 커스텀 Grok 패턴 정의
- **README**: `README.md` - 프로젝트 개요 및 사용법