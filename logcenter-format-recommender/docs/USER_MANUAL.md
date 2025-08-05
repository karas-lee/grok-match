# LogCenter Format Recommender 사용자 매뉴얼

## 목차
1. [소개](#소개)
2. [설치 가이드](#설치-가이드)
3. [기본 사용법](#기본-사용법)
4. [고급 기능](#고급-기능)
5. [API 클라이언트](#api-클라이언트)
6. [문제 해결](#문제-해결)
7. [성능 최적화](#성능-최적화)
8. [부록](#부록)

## 소개

LogCenter Format Recommender는 SIEM 시스템에서 다양한 로그 형식을 자동으로 인식하고 분류하는 도구입니다. Grok 패턴 매칭 기술을 사용하여 458개의 사전 정의된 로그 포맷을 지원합니다.

### 주요 기능
- 🔍 **자동 로그 포맷 인식**: 로그 샘플을 분석하여 가장 적합한 포맷 추천
- 🚀 **고성능 처리**: 병렬 처리를 통한 대용량 로그 빠른 분석
- 📊 **다양한 출력 형식**: TEXT, JSON, CSV 형식 지원
- 🔌 **API 클라이언트**: 원격 서버와 통신하여 중앙 집중식 관리
- 💾 **스마트 캐싱**: 반복 요청에 대한 빠른 응답

## 설치 가이드

### 시스템 요구사항
- **운영체제**: Windows, Linux, macOS
- **Java**: JDK 8 이상
- **메모리**: 최소 512MB, 권장 1GB
- **디스크**: 최소 100MB

### 설치 방법

#### 1. 사전 빌드된 JAR 다운로드
```bash
# GitHub 릴리즈에서 다운로드
wget https://github.com/your-repo/releases/download/v1.0.0/logcenter-format-recommender-1.0.0.jar
```

#### 2. 소스에서 빌드
```bash
# 저장소 클론
git clone https://github.com/your-repo/logcenter-format-recommender.git
cd logcenter-format-recommender

# 빌드
mvn clean package

# 실행 파일 확인
ls -la target/logcenter-format-recommender-*.jar
```

### 설정 파일

#### application.properties
```properties
# 로그 레벨 설정
logging.level.root=INFO
logging.level.com.logcenter=DEBUG

# 병렬 처리 스레드 수
recommender.threads=8

# 캐시 설정
cache.enabled=true
cache.ttl.minutes=60
```

#### api.properties (API 클라이언트용)
```properties
# API 서버 설정
api.url=http://localhost:8080
api.key=your-api-key
api.enabled=false

# 연결 설정
api.connection.timeout=5
api.read.timeout=30
api.max.retries=3
```

## 기본 사용법

### 단일 로그 분석
```bash
# 직접 로그 문자열 입력
java -jar logcenter-format-recommender.jar "192.168.1.100 - - [05/Aug/2025:10:15:30 +0900] \"GET /index.html HTTP/1.1\" 200 1234"

# 결과
추천 로그 포맷:
1. APACHE_HTTP (신뢰도: 98.0%)
   - 벤더: APACHE
   - 그룹: WEBSERVER
   - 매칭 필드: client_ip, timestamp, method, path, status, bytes
```

### 파일 분석
```bash
# 단일 파일 분석
java -jar logcenter-format-recommender.jar -f /var/log/apache2/access.log

# 여러 옵션 조합
java -jar logcenter-format-recommender.jar -f server.log -n 10 -m 80 --detail
```

### 디렉토리 분석
```bash
# 디렉토리 내 모든 로그 파일 분석
java -jar logcenter-format-recommender.jar -d /var/log --stats

# 특정 확장자만 분석
java -jar logcenter-format-recommender.jar -d /logs -g FIREWALL
```

### 출력 형식

#### TEXT 형식 (기본)
```
=== 로그 포맷 추천 결과 ===
로그: 192.168.1.100 - - [05/Aug/2025:10:15:30 +0900] "GET /index.html HTTP/1.1" 200 1234

추천 포맷:
1. APACHE_HTTP (98.0%)
   - 벤더: APACHE
   - 그룹: WEBSERVER
   
2. NGINX_ACCESS (92.5%)
   - 벤더: NGINX
   - 그룹: WEBSERVER
```

#### JSON 형식
```bash
java -jar logcenter-format-recommender.jar -f log.txt -o json

# 결과
{
  "log": "192.168.1.100 - - [05/Aug/2025:10:15:30 +0900] \"GET /index.html HTTP/1.1\" 200 1234",
  "recommendations": [
    {
      "formatName": "APACHE_HTTP",
      "confidence": 98.0,
      "vendor": "APACHE",
      "group": "WEBSERVER",
      "matchedFields": {
        "client_ip": "192.168.1.100",
        "timestamp": "05/Aug/2025:10:15:30 +0900",
        "method": "GET",
        "path": "/index.html",
        "status": "200",
        "bytes": "1234"
      }
    }
  ]
}
```

#### CSV 형식
```bash
java -jar logcenter-format-recommender.jar -f logs.txt -o csv > results.csv

# 결과
LogSample,FormatName,Confidence,Vendor,Group
"192.168.1.100 - - [...]","APACHE_HTTP",98.0,"APACHE","WEBSERVER"
"192.168.1.100 - - [...]","NGINX_ACCESS",92.5,"NGINX","WEBSERVER"
```

## 고급 기능

### 필터링 옵션

#### 그룹별 필터링
```bash
# FIREWALL 그룹만 검색
java -jar logcenter-format-recommender.jar -f security.log -g FIREWALL

# 사용 가능한 그룹 확인
java -jar logcenter-format-recommender.jar --list-groups
```

#### 벤더별 필터링
```bash
# Cisco 장비 로그만 검색
java -jar logcenter-format-recommender.jar -f network.log -v "CISCO SYSTEMS"

# 사용 가능한 벤더 확인
java -jar logcenter-format-recommender.jar --list-vendors
```

### 신뢰도 설정
```bash
# 신뢰도 85% 이상만 표시
java -jar logcenter-format-recommender.jar -f app.log -m 85

# 상위 3개만 표시
java -jar logcenter-format-recommender.jar -f app.log -n 3
```

### 통계 정보
```bash
# 통계 정보와 함께 출력
java -jar logcenter-format-recommender.jar -f logs/ -d --stats

# 결과
=== 분석 통계 ===
- 전체 로그: 1,000개
- 매칭 성공: 950개 (95.0%)
- 매칭 실패: 50개 (5.0%)
- 평균 처리 시간: 10ms/로그
- 가장 많이 매칭된 포맷:
  1. APACHE_HTTP: 450개 (45.0%)
  2. LINUX_SYSLOG: 200개 (20.0%)
  3. CISCO_ASA: 150개 (15.0%)
```

## API 클라이언트

### 환경 설정

#### 환경변수 사용
```bash
export LOGCENTER_API_URL=http://api.example.com
export LOGCENTER_API_KEY=your-api-key

java -jar logcenter-format-recommender.jar --api "로그 샘플"
```

#### 명령행 옵션
```bash
java -jar logcenter-format-recommender.jar \
  --api \
  --api-url http://api.example.com \
  --api-key your-api-key \
  "로그 샘플"
```

### API 기능
- **중앙 집중식 포맷 관리**: 서버에서 최신 포맷 정의 가져오기
- **캐싱**: 자주 사용되는 결과 로컬 캐싱
- **Health Check**: 서버 상태 자동 확인
- **재시도 로직**: 네트워크 오류 시 자동 재시도

## 문제 해결

### 일반적인 문제

#### 1. OutOfMemoryError
```bash
# 힙 메모리 증가
java -Xmx1g -jar logcenter-format-recommender.jar -f large.log
```

#### 2. 패턴 컴파일 오류
```
오류: Grok 패턴 컴파일 실패 - Unclosed character class
해결: pattern_error.md 파일에서 문제가 있는 패턴 확인
```

#### 3. 느린 처리 속도
```bash
# 병렬 처리 비활성화 (디버깅용)
java -Drecommender.parallel=false -jar logcenter-format-recommender.jar
```

### 디버깅 모드
```bash
# 상세 로그 활성화
java -Dlogging.level.com.logcenter=DEBUG -jar logcenter-format-recommender.jar

# 특정 클래스만 디버그
java -Dlogging.level.com.logcenter.recommender.matcher=TRACE -jar logcenter-format-recommender.jar
```

## 성능 최적화

### 메모리 최적화
```bash
# 권장 JVM 설정
java -Xms512m -Xmx1g \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -jar logcenter-format-recommender.jar
```

### 병렬 처리 튜닝
```bash
# CPU 코어 수에 따라 스레드 조정
java -Drecommender.threads=16 -jar logcenter-format-recommender.jar
```

### 캐시 설정
```bash
# 캐시 크기 및 TTL 조정
java -Dcache.max.size=10000 \
     -Dcache.ttl.minutes=120 \
     -jar logcenter-format-recommender.jar
```

## 부록

### A. 지원 로그 포맷 목록
```bash
# 전체 포맷 목록 확인
java -jar logcenter-format-recommender.jar --list-formats

# 특정 그룹만 확인
java -jar logcenter-format-recommender.jar --list-formats -g FIREWALL
```

### B. Grok 패턴 커스터마이징
1. `resources/custom-grok-patterns` 파일 수정
2. 새로운 패턴 추가:
   ```
   CUSTOM_IP (?<custom_ip>\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3})
   CUSTOM_DATE (?<custom_date>\d{4}-\d{2}-\d{2})
   ```
3. 재빌드 및 테스트

### C. 성능 벤치마크
| 로그 수 | 처리 시간 | 메모리 사용 | CPU 사용률 |
|---------|-----------|-------------|------------|
| 100     | 0.5초     | 150MB       | 25%        |
| 1,000   | 1초       | 200MB       | 50%        |
| 10,000  | 5초       | 300MB       | 75%        |
| 100,000 | 45초      | 500MB       | 90%        |

### D. 오류 코드
| 코드 | 설명 | 해결 방법 |
|------|------|-----------|
| 1    | 일반 오류 | 로그 확인 |
| 2    | 파일 없음 | 파일 경로 확인 |
| 3    | 권한 없음 | 파일 권한 확인 |
| 4    | 메모리 부족 | JVM 힙 크기 증가 |
| 5    | API 연결 실패 | 네트워크 및 API 설정 확인 |

---
© 2025 LogCenter. All rights reserved.