# 로그 포맷 검증 도구 사용 가이드

## 목차
1. [개요](#개요)
2. [설치 및 실행](#설치-및-실행)
3. [명령줄 옵션](#명령줄-옵션)
4. [검증 항목](#검증-항목)
5. [리포트 형식](#리포트-형식)
6. [사용 예제](#사용-예제)
7. [문제 해결](#문제-해결)

## 개요

로그 포맷 검증 도구는 `setting_logformat.json` 파일에 정의된 모든 로그 포맷을 사전에 검증하여, 배포 전에 패턴 컴파일 오류나 파싱 문제를 미리 발견할 수 있도록 돕는 도구입니다.

### 주요 기능
- **패턴 컴파일 검증**: Grok 패턴의 문법 오류 검출
- **샘플 매칭 테스트**: 샘플 로그와 패턴의 실제 매칭 확인
- **품질 검사**: 필드 수, GREEDYDATA 사용 등 품질 이슈 경고
- **다양한 리포트 형식**: 텍스트, HTML, JSON 형식 지원
- **병렬 처리**: 대량의 포맷을 빠르게 검증

## 설치 및 실행

### 사전 요구사항
- Java 8 이상
- Maven 3.6 이상 (빌드 시)
- 512MB 이상의 메모리

### 빌드
```bash
# 프로젝트 빌드
mvn clean package -DskipTests

# 빌드 결과물
# target/logcenter-format-recommender-1.0.0-jar-with-dependencies.jar
```

### 기본 실행
```bash
# 기본 검증 실행 (텍스트 출력)
java -cp target/logcenter-format-recommender-1.0.0-jar-with-dependencies.jar \
  com.logcenter.recommender.validator.ValidatorMain
```

## 명령줄 옵션

| 옵션 | 설명 | 기본값 |
|------|------|--------|
| `-f, --file <path>` | 검증할 포맷 파일 경로 | `/setting_logformat.json` |
| `-o, --output <file>` | 결과를 파일로 저장 | 콘솔 출력 |
| `--format <type>` | 출력 형식 (text/html/json) | `text` |
| `-h, --help` | 도움말 표시 | - |

## 검증 항목

### 1. 오류 (FAIL)
검증이 실패하는 심각한 문제:
- **패턴 컴파일 오류**: 잘못된 Grok 문법
- **미정의 패턴 참조**: 존재하지 않는 패턴 사용
- **샘플 매칭 실패**: 샘플 로그와 패턴 불일치
- **타임아웃**: 10초 이상 소요되는 패턴

### 2. 경고 (WARNING)
성능이나 품질에 영향을 줄 수 있는 문제:
- **필드 수 부족**: 유효 필드가 2개 이하
- **GREEDYDATA 사용**: 성능 저하 가능성
- **너무 일반적인 패턴**: `^.*$` 같은 패턴
- **샘플 로그 누락**: 실제 테스트 불가

### 3. 통과 (PASS)
모든 검증을 통과한 정상 포맷

## 리포트 형식

### 1. 텍스트 형식 (기본)
```
=============================================================
                로그 포맷 검증 리포트
                생성 시간: 2025-08-06 10:00:00
=============================================================

■ 검증 통계
---------------------------------------------------------
전체 포맷 수: 324
✓ 통과: 298
⚠ 경고: 20
✗ 실패: 6
— 건너뜀: 0

성공률: 92.59%

■ 그룹별 통계
---------------------------------------------------------
FIREWALL            : 전체  69 | 통과  65 | 경고   3 | 실패   1
WEBSERVER           : 전체  12 | 통과  10 | 경고   2 | 실패   0
SYSTEM              : 전체  33 | 통과  30 | 경고   2 | 실패   1
...
```

### 2. HTML 형식
웹 브라우저에서 볼 수 있는 상세 리포트:
- 색상으로 구분된 상태 표시
- 정렬 가능한 테이블
- 상세 오류 메시지

### 3. JSON 형식
CI/CD 파이프라인 통합용:
```json
{
  "generatedAt": "2025-08-06 10:00:00",
  "totalFormats": 324,
  "statistics": {
    "passed": 298,
    "warnings": 20,
    "failed": 6,
    "skipped": 0
  },
  "successRate": 92.59,
  "results": [...]
}
```

## 사용 예제

### 예제 1: 기본 검증
```bash
java -cp target/logcenter-format-recommender-1.0.0-jar-with-dependencies.jar \
  com.logcenter.recommender.validator.ValidatorMain
```

### 예제 2: HTML 리포트 생성
```bash
java -cp target/logcenter-format-recommender-1.0.0-jar-with-dependencies.jar \
  com.logcenter.recommender.validator.ValidatorMain \
  --format html \
  -o validation-report.html

# 브라우저에서 열기
open validation-report.html  # macOS
xdg-open validation-report.html  # Linux
start validation-report.html  # Windows
```

### 예제 3: CI/CD 통합
```bash
#!/bin/bash
# CI/CD 스크립트 예제

# JSON 형식으로 검증 결과 저장
java -cp target/logcenter-format-recommender-1.0.0-jar-with-dependencies.jar \
  com.logcenter.recommender.validator.ValidatorMain \
  --format json \
  -o validation-result.json

# 종료 코드 확인 (실패가 있으면 1)
if [ $? -ne 0 ]; then
  echo "로그 포맷 검증 실패!"
  exit 1
fi

echo "모든 로그 포맷 검증 통과"
```

### 예제 4: 커스텀 포맷 파일 검증
```bash
# 다른 경로의 포맷 파일 검증
java -cp target/logcenter-format-recommender-1.0.0-jar-with-dependencies.jar \
  com.logcenter.recommender.validator.ValidatorMain \
  -f /custom/path/my_formats.json
```

### 예제 5: 메모리 설정과 함께 실행
```bash
# 대량의 포맷 검증 시 메모리 증가
java -Xmx2G -Xms512M \
  -cp target/logcenter-format-recommender-1.0.0-jar-with-dependencies.jar \
  com.logcenter.recommender.validator.ValidatorMain
```

## 문제 해결

### 1. OutOfMemoryError
**증상**: `java.lang.OutOfMemoryError: Java heap space`

**해결**:
```bash
# 힙 메모리 증가
java -Xmx4G -cp target/logcenter-format-recommender-1.0.0-jar-with-dependencies.jar \
  com.logcenter.recommender.validator.ValidatorMain
```

### 2. 타임아웃 발생
**증상**: 특정 포맷에서 타임아웃 오류

**원인**: 복잡한 정규식 패턴이나 백트래킹 문제

**해결**: 
- 해당 패턴 단순화
- GREEDYDATA 사용 최소화
- 앵커(^, $) 사용으로 매칭 범위 제한

### 3. 패턴 컴파일 오류
**증상**: "패턴 컴파일 오류" 메시지

**일반적인 원인**:
- 이스케이프 누락: `\[` 대신 `[` 사용
- 미정의 패턴 참조: `%{UNDEFINED_PATTERN}`
- 잘못된 그룹 이름: 숫자로 시작하거나 특수문자 포함

**디버깅 방법**:
```bash
# 특정 포맷만 테스트
java -cp target/logcenter-format-recommender-1.0.0-jar-with-dependencies.jar \
  com.logcenter.recommender.Main \
  "테스트할 로그 라인" \
  "exp_name": "문제가 되는 포맷 이름"
```

### 4. 샘플 매칭 실패
**증상**: "샘플 로그와 패턴이 매칭되지 않음"

**확인 사항**:
1. 샘플 로그의 공백 문자 확인 (탭 vs 스페이스)
2. 날짜 형식 일치 여부
3. 필수 필드 존재 여부
4. 시작/종료 앵커 확인

## 모범 사례

### 1. 정기적인 검증
- 새로운 포맷 추가 시 즉시 검증
- 배포 전 전체 검증 실행
- CI/CD 파이프라인에 통합

### 2. 성능 최적화
- GREEDYDATA 사용 최소화
- 구체적인 패턴 사용 (예: `\d{4}` vs `.*`)
- 앵커 사용으로 매칭 범위 제한

### 3. 품질 관리
- 모든 포맷에 샘플 로그 포함
- 최소 3개 이상의 유효 필드 추출
- 의미 있는 필드명 사용

### 4. 문서화
- 복잡한 패턴에 주석 추가
- 필드 의미 설명 포함
- 변경 이력 관리

## 추가 도구

### 포맷 추천 테스트
```bash
# 특정 로그에 대한 포맷 추천 테스트
java -jar target/logcenter-format-recommender-1.0.0-jar-with-dependencies.jar \
  "2024-01-01 10:00:00 192.168.1.1 allow"
```

### 패턴 디버깅
```bash
# 특정 패턴의 상세 디버그 정보 출력
java -Dlog.level=DEBUG \
  -cp target/logcenter-format-recommender-1.0.0-jar-with-dependencies.jar \
  com.logcenter.recommender.validator.ValidatorMain
```

## 참고 자료

- [Grok 패턴 문법](https://www.elastic.co/guide/en/logstash/current/plugins-filters-grok.html)
- [정규표현식 가이드](https://regex101.com/)
- [프로젝트 README](../README.md)
- [PRD 문서](./LOGCENTER-LOG-FORMAT-RECOMMEND-PRD.md)

## 지원 및 문의

문제 발생 시 다음 정보와 함께 보고해 주세요:
- 검증 리포트 (JSON 형식 권장)
- 문제가 되는 포맷의 grok_exp
- 샘플 로그
- 오류 메시지 전문

---

*최종 업데이트: 2025-08-06*