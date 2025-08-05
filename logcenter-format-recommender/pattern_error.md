# Grok 패턴 오류 분석 및 해결 방안

## 개요
- 전체 패턴: 458개
- 성공: 419개 (91.5%)
- 실패: 39개 (컴파일 실패 7개, 매칭 실패 32개)

---

## 1. 컴파일 실패 패턴 (7개)

### 1.1 SECUI_BLUEMAX_NGF_1.00_CSV 시리즈

#### 영향받는 패턴
- SECUI_BLUEMAX_NGF_1.00_CSV_1
- SECUI_BLUEMAX_NGF_1.00_CSV_2
- SECUI_BLUEMAX_NGF_1.00_CSV_3
- SECUI_BLUEMAX_NGF_1.00_CSV_4
- SECUI_BLUEMAX_NGF_1.00_CSV_5
- SECUI_BLUEMAX_NGF_1.00_CSV_6
- SECUI_BLUEMAX_NGF_1.00_CSV_7

#### 문제 원인
```
패턴: [^\[]*\[(?<log_name>fw4_deny)]*
오류: Unclosed character class near index
```
- `[(?<log_name>fw4_deny)]*` 형태의 잘못된 정규식 구문
- PatternNormalizer가 `\\[fw4_deny\\]`로 변환하지만, 전체 패턴에서 `[^\[]*\\[fw4_deny\\]`가 되어 `[^\[`가 닫히지 않은 문자 클래스로 해석됨

#### 해결 방안
JSON 파일에서 다음과 같이 수정:
```json
// 현재 (잘못된 형태)
"grok_exp": "%{TEXT1:log_time}\\s<%{TEXT17:pri}>[^\\[]*\\[(?<log_name>fw4_deny)]*\\s..."

// 수정 후
"grok_exp": "%{TEXT1:log_time}\\s<%{TEXT17:pri}>[^\\\\[]*\\\\[fw4_deny\\\\]\\s..."
```

또는 named group이 필요한 경우:
```json
"grok_exp": "%{TEXT1:log_time}\\s<%{TEXT17:pri}>.*?\\[(?<log_name>fw4_deny)\\]\\s..."
```

---

## 2. 매칭 실패 패턴 (32개)

### 2.1 TREND_MICRO_TIPPINGPOINT 시리즈

#### TREND_MICRO_TIPPINGPOINT_1.00_1
- **문제**: 날짜 형식 불일치
- **패턴**: `^%{LOG_TIME:log_time} <%{PRI:pri}>%{DATE_FORMAT3} %{SKIP}...`
- **샘플**: `20130828141955 <5>AUG 28 14:45:52 2013  myhostname...`
- **원인**: DATE_FORMAT3 패턴이 "AUG 28 14:45:52"를 매칭하지 못함
- **해결**: DATE_FORMAT3 패턴 정의 확인 또는 적절한 날짜 패턴으로 변경

#### TREND_MICRO_TIPPINGPOINT_1.00_2, TREND_MICRO_TIPPINGPOINT_1.00_3
- 동일한 날짜 형식 문제

### 2.2 NGINX 시리즈

#### NGINX_NGINX_1.00_1
- **문제**: 패턴 구조 불일치
- **패턴**: `^%{TEXT1:dst_ip}.*\[%{DATE_FORMAT8:log_time}\]\s.*\s%{NUMBER:status}\s%{NUMBER:sent_size}\s%{BATCH:message}$`
- **샘플**: `47.29.201.179 - - [28/Feb/2019:13:17:10 +0000] "GET /?p=1 HTTP/2.0" 200 5316...`
- **원인**: TEXT1 (`[^\s]*`)이 IP만 매칭하고, 중간의 "- -" 부분 처리 누락
- **해결**: 
  ```json
  "grok_exp": "^%{IP:dst_ip}\\s-\\s-\\s\\[%{DATE_FORMAT8:log_time}\\]\\s.*?\\s%{NUMBER:status}\\s%{NUMBER:sent_size}\\s%{BATCH:message}$"
  ```

#### NGINX_NGINX_1.00_2, NGINX_NGINX_1.00_3
- 유사한 패턴 구조 문제

### 2.3 데이터베이스 로그 시리즈

#### MYSQL_MYSQL_1.00_1
- **문제**: 날짜 형식 및 옵셔널 필드 처리
- **패턴**: `^(?:%{DATE_FORMAT7:log_time})\s(%{NUMBER:thread}\s)?(\[%{TEXT1:level}\]\s)?%{BATCH:message}$`
- **샘플**: `2016-12-09T12:08:33.335060Z 0 [Warning] TIMESTAMP with...`
- **원인**: DATE_FORMAT7이 ISO 형식 타임스탬프를 매칭하지 못함
- **해결**: MySQL 타임스탬프 형식에 맞는 패턴 사용
  ```json
  "grok_exp": "^%{TIMESTAMP_ISO8601:log_time}\\s+%{NUMBER:thread}\\s+\\[%{WORD:level}\\]\\s+%{GREEDYDATA:message}$"
  ```

#### MYSQL_MYSQL_1.00_2
- 유사한 타임스탬프 형식 문제

#### POSTGRESQL_POSTGRESQL_1.00_1
- **문제**: PostgreSQL 특유의 로그 형식
- **해결**: PostgreSQL 로그 형식에 맞는 패턴 재정의 필요

#### MSSQL_MSSQL_1.00_1
- **문제**: MSSQL 특유의 로그 형식
- **해결**: MSSQL 로그 형식에 맞는 패턴 재정의 필요

### 2.4 기타 벤더 패턴

#### PENTASECURITY_WAPPLE_1.00_WELF_3
- **문제**: WELF 형식 파싱 오류
- **해결**: WELF(WebTrends Enhanced Log Format) 구조에 맞게 패턴 수정

#### SECUI_MF2_1.00_7
- **문제**: 필드 구분자 불일치
- **해결**: 실제 로그의 구분자 확인 후 패턴 수정

#### IBM_AIX_1.00_2
- **문제**: AIX 시스템 로그 형식 불일치
- **해결**: AIX syslog 형식에 맞게 패턴 조정

---

## 3. 권장 사항

### 3.1 즉시 수정 가능한 항목
1. **SECUI_BLUEMAX_NGF CSV 시리즈**: 정규식 구문 오류 수정
2. **NGINX 시리즈**: 중간 필드 처리 추가
3. **데이터베이스 로그**: 표준 타임스탬프 패턴 사용

### 3.2 추가 확인이 필요한 항목
1. 각 벤더별 실제 로그 샘플 재확인
2. 커스텀 패턴(DATE_FORMAT1~10) 정의 검증
3. 옵셔널 필드 처리 로직 개선

### 3.3 개선 방안
1. **패턴 검증 도구 개발**: 패턴 작성 시 실시간 검증
2. **샘플 로그 자동 생성**: 패턴으로부터 예상 로그 생성
3. **패턴 버전 관리**: 벤더별, 버전별 패턴 관리 체계 구축

---

## 4. 테스트 명령어

개별 패턴 테스트:
```bash
mvn exec:java -Dexec.mainClass="com.logcenter.recommender.test.DetailedPatternTester" -Dexec.cleanupDaemonThreads=false
```

전체 패턴 테스트:
```bash
mvn exec:java -Dexec.mainClass="com.logcenter.recommender.test.PatternTestRunner" -Dexec.cleanupDaemonThreads=false
```

---

*마지막 업데이트: 2025-08-05*