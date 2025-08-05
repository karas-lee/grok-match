# CLAUDE.md

이 파일은 이 저장소에서 작업할 때 Claude Code (claude.ai/code)에게 지침을 제공합니다.

**중요**: 모든 문서 및 답변은 한글로 작성 및 답변을 합니다.

## 프로젝트 개요

LogCenter Format Recommendation System (LFRS) - Grok 패턴 매칭을 사용하여 100개 이상의 사전 정의된 포맷 중에서 최적의 로그 포맷을 자동으로 식별하는 SIEM 로그 포맷 추천 시스템입니다.

## 핵심 아키텍처

### 코어 매칭 알고리즘
시스템은 Grok 완전 매칭을 우선시합니다:
- **단일 완전 매칭**: 98% 신뢰도로 즉시 반환
- **다중 완전 매칭**: 필드 수, GREEDYDATA 사용, 패턴 구체성을 기반으로 평가 (95-97%)
- **부분 매칭만 존재**: 최대 70% 신뢰도로 제한적 평가
- **매칭 없음**: 빈 결과 반환

### 핵심 데이터 파일
1. **`docs/custom-grok-patterns`**: 233개의 커스텀 Grok 패턴 (`src/main/resources/`로 복사 필요)
   - TEXT 패턴 (TEXT1-TEXT18)
   - IP/Port 패턴 (SRC_IP, DST_IP 등)
   - Date/Time 패턴 (DATE_FORMAT1-10)
   - 특수 패턴 (CISCO, SKIP, EMAIL 등)

2. **`docs/GROK-PATTERN-CONVERTER.sql`**: Grok 패턴이 포함된 100개의 로그 포맷 정의 (`src/main/resources/`로 복사 필요)
   - 그룹: Firewall (23), Web Server (4), System (11), WAF (5), IPS (8), Application (20) 등

## 빌드 및 개발 명령어

### Maven 빌드
```bash
# 테스트 포함 클린 빌드
mvn clean install

# 테스트 없이 빌드
mvn clean package -DskipTests

# 특정 테스트 실행
mvn test -Dtest=GrokMatcherTest

# 특정 프로파일로 빌드
mvn clean package -Pdev   # 개발 환경
mvn clean package -Pprod  # 운영 환경
```

### 애플리케이션 실행
```bash
# 의존성 포함 실행
java -jar target/logcenter-format-recommender-1.0.0-jar-with-dependencies.jar

# 메모리 설정과 함께 실행
java -Xmx2G -Xms512M -jar target/logcenter-format-recommender-1.0.0-jar-with-dependencies.jar
```

### 개발 유틸리티
```bash
# 의존성 확인
mvn dependency:tree

# JavaDoc 생성
mvn javadoc:javadoc

# 프로젝트 구조 확인
mvn help:effective-pom
```

## 개발 워크플로우

각 단계는 다음을 따릅니다: **개발 → 단위 테스트 → 코드 리뷰 → 수정 → 통합 테스트 → 커밋**

현재 진행률 추적: `task/PROGRESS_FORMAT_RECOMMEND.md`

## 기술적 제약사항

- **JDK**: 반드시 1.8 호환
- **Grok 라이브러리**: io.krakens:java-grok:0.1.9 (JDK 1.8 호환)
- **JSON 처리**: JDK 1.8 호환성을 위해 Gson 사용 (Jackson 사용 불가)
- **리소스 로딩**: JAR 호환성을 위해 `ClassLoader.getResourceAsStream()` 사용

## Maven 프로젝트 구조

```
logcenter-format-recommender/
├── pom.xml
├── src/main/java/com/logcenter/recommender/
│   ├── Main.java              # 진입점
│   ├── config/                # 설정 관리
│   ├── model/                 # 데이터 모델
│   ├── service/               # 핵심 서비스
│   │   ├── GrokMatcher.java   # 패턴 매칭 엔진
│   │   └── Recommender.java   # 추천 서비스
│   └── util/                  # 유틸리티
└── src/main/resources/
    ├── custom-grok-patterns   # 233개 커스텀 패턴
    └── GROK-PATTERN-CONVERTER.sql  # 100개 로그 포맷
```

## 중요 구현 참고사항

1. **커스텀 패턴 로딩**: 표준 패턴보다 먼저 `custom-grok-patterns` 파일에서 233개 패턴을 모두 로드해야 함
2. **패턴 우선순위**: 이름 충돌 시 커스텀 패턴이 표준 패턴보다 우선
3. **병렬 처리**: 성능 향상을 위해 패턴 매칭에 병렬 스트림 사용
4. **캐싱**: 재컴파일 방지를 위한 패턴 컴파일 캐싱 구현
5. **메모리**: 기본 힙 크기는 최소 512MB, 운영 환경에서는 2GB 권장

## Python 유틸리티

`script/grok-converter.py`: LOGCENTER-LOG-FORMAT.sql (1.2MB)에서 `data_table` 항목을 제거하여 GROK-PATTERN-CONVERTER.sql (366KB) 생성

## 주요 문서

- **PRD 문서**: `docs/LOGCENTER-LOG-FORMAT-RECOMMEND-PRD.md` - 상세 제품 요구사항
- **진행률 추적**: `task/PROGRESS_FORMAT_RECOMMEND.md` - 개발 체크리스트 및 진행 상황
- **커스텀 패턴**: `docs/custom-grok-patterns` - 233개 커스텀 Grok 패턴 정의