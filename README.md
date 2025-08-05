# LogCenter Format Recommender

SIEM 제품을 위한 로그 포맷 추천 시스템입니다. 사용자가 로그 샘플을 입력하면 Grok 패턴 매칭을 통해 가장 적합한 로그 포맷을 추천합니다.

## 프로젝트 개요

LogCenter Format Recommender(LFRS)는 100개 이상의 사전 정의된 로그 포맷 중에서 사용자의 로그 샘플과 가장 일치하는 포맷을 자동으로 찾아주는 도구입니다.

### 주요 기능

- **자동 로그 포맷 인식**: Grok 패턴 매칭을 통한 정확한 포맷 식별
- **다양한 로그 지원**: Firewall, Web Server, System, WAF, IPS 등 13개 카테고리의 로그 포맷 지원
- **커스텀 패턴**: 233개의 커스텀 Grok 패턴으로 정확도 향상
- **신뢰도 점수**: 매칭 결과에 대한 신뢰도 백분율 제공
- **Java 1.8 호환**: 레거시 시스템에서도 사용 가능
- **영구 캐싱**: 컴파일된 패턴과 로그 포맷을 캐싱하여 초기화 시간 단축

## 시스템 요구사항

- Java 1.8 이상
- Maven 3.6 이상
- 메모리: 최소 512MB

## 프로젝트 구조

```
logcenter-format-recommender/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/logcenter/recommender/
│   │   │       ├── model/          # 데이터 모델
│   │   │       ├── config/         # 설정 관리
│   │   │       ├── util/           # 유틸리티
│   │   │       ├── cache/          # 영구 캐시 관리
│   │   │       ├── grok/           # Grok 엔진 (Phase 3)
│   │   │       ├── matcher/        # 매칭 엔진 (Phase 4)
│   │   │       ├── service/        # 추천 서비스 (Phase 5)
│   │   │       └── cli/            # CLI 인터페이스 (Phase 6)
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── logback.xml
│   │       ├── custom-grok-patterns    # 233개 커스텀 패턴
│   │       └── GROK-PATTERN-CONVERTER.sql  # 100개 로그 포맷 정의
│   └── test/
├── docs/
│   ├── LOGCENTER-LOG-FORMAT-RECOMMEND-PRD.md
│   └── custom-grok-patterns
├── task/
│   └── PROGRESS_FORMAT_RECOMMEND.md
└── pom.xml
```

## 빌드 및 실행

### 빌드

```bash
mvn clean package
```

### 테스트

```bash
mvn test
```

### 실행

```bash
# 단일 로그 분석
java -jar target/logcenter-format-recommender-1.0.0-jar-with-dependencies.jar "로그 샘플"

# 파일에서 로그 읽기
java -jar target/logcenter-format-recommender-1.0.0-jar-with-dependencies.jar -f /path/to/logfile.log

# 상위 10개 결과 표시
java -jar target/logcenter-format-recommender-1.0.0-jar-with-dependencies.jar -n 10 "로그 샘플"

# 캐시 사용 옵션
java -jar target/logcenter-format-recommender-1.0.0-jar-with-dependencies.jar --no-cache "로그 샘플"
java -jar target/logcenter-format-recommender-1.0.0-jar-with-dependencies.jar --cache-dir /custom/cache/path "로그 샘플"
java -jar target/logcenter-format-recommender-1.0.0-jar-with-dependencies.jar --clear-cache
java -jar target/logcenter-format-recommender-1.0.0-jar-with-dependencies.jar --rebuild-cache
```

## 영구 캐시 시스템

LFRS는 성능 향상을 위해 영구 캐시 시스템을 제공합니다:

### 캐시 특징
- **자동 캐시 생성**: 첫 실행 시 자동으로 캐시 생성
- **체크섬 검증**: 원본 파일 변경 시 자동으로 캐시 재생성
- **TTL 지원**: 설정된 기간 후 자동으로 캐시 만료
- **캐시 위치**: 기본값 `~/.logcenter/cache/`

### 캐시 설정 (application.properties)
```properties
cache.persistent.enabled=true
cache.persistent.dir=.logcenter/cache
cache.persistent.ttl.days=7
cache.persistent.checksum.enabled=true
```

### 성능 향상
- 첫 실행: 약 2-3초 (패턴 컴파일 및 캐시 생성)
- 이후 실행: 약 0.5초 이하 (캐시에서 로드)

## 매칭 알고리즘

LFRS는 필드 특성을 고려한 개선된 매칭 알고리즘을 사용합니다:

1. **완전 매칭 신뢰도**:
   - 구체적 필드 5개 이상: 98%
   - 구체적 필드 3-4개: 96%
   - 구체적 필드 1-2개: 94%
   - 일반 필드만: 92%
   - 필드 2개 이하: 88%

2. **필드 품질 평가**:
   - 구체적 필드: src_ip, dst_ip, protocol, action 등
   - 제외 필드: log_time, message (신뢰도 계산에서 제외)

3. **부분 매칭**: 일부만 매칭되면 최대 70% 신뢰도

## 지원 로그 포맷

### 주요 카테고리 (13개)

1. **Firewall** (23개): Fortinet, Palo Alto, Cisco ASA 등
2. **Web Server** (4개): Apache, Nginx, IIS, Tomcat
3. **System** (11개): Linux, Windows, AIX 등
4. **WAF** (14개): F5, Imperva, Cloudflare 등
5. **IPS** (8개): Snort, TippingPoint, McAfee 등
6. **Application** (18개): Oracle, MySQL, PostgreSQL 등
7. 기타 카테고리...

### 커스텀 Grok 패턴

- **TEXT 패턴** (18개): TEXT1-TEXT18
- **숫자 패턴** (28개): COUNT, FILE_SIZE, PID 등
- **IP/MAC/Port 패턴** (12개): SRC_IP, DST_IP, MAC_ADDR 등
- **날짜/시간 패턴** (10개): DATE_FORMAT1-10, LOG_TIME
- **이메일 패턴** (3개): MAIL, SENDER, RECEIVER
- **예약어 패턴** (13개): RESERVED0-12
- **Cisco 특화 패턴** (13개): CISCO1-13

## 개발 진행 상황

전체 진행률: **85%**

- [x] Phase 1: 프로젝트 설정 및 기반 구축
- [x] Phase 2: 핵심 모델 및 유틸리티 개발
- [x] Phase 3: Grok 엔진 통합
- [x] Phase 4: 매칭 엔진 구현
- [x] Phase 5: 추천 서비스 구현
- [x] Phase 6: CLI 인터페이스 개발
- [x] Phase 7: 영구 캐시 시스템 구현
- [x] Phase 8: 테스트 구현
- [ ] Phase 9: 문서화 및 배포 준비 (진행 중)
- [ ] Phase 10: 배포 및 운영

자세한 진행 상황은 [PROGRESS_FORMAT_RECOMMEND.md](task/PROGRESS_FORMAT_RECOMMEND.md)를 참조하세요.

## 기여 방법

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 라이선스

이 프로젝트는 내부 사용을 위해 개발되었습니다. 라이선스 정보는 추후 결정될 예정입니다.

## 연락처

프로젝트 관련 문의사항은 이슈 트래커를 이용해주세요.

---

🤖 Generated with [Claude Code](https://claude.ai/code)

Co-Authored-By: Claude <noreply@anthropic.com>