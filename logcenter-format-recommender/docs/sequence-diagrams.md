# 로그 포맷 추천 시스템 시퀀스 다이어그램

## 1. 전체 시스템 플로우

```mermaid
sequenceDiagram
    participant User as 사용자
    participant CLI as CliCommand
    participant Main as Main
    participant Service as LogFormatRecommenderImpl
    participant Cache as PersistentCacheManager
    participant Matcher as AdvancedLogMatcher
    participant Grok as GrokCompilerWrapper
    participant Filter as PatternFilter
    participant API as ApiClient

    User->>CLI: 로그 입력
    CLI->>CLI: 명령행 옵션 파싱
    CLI->>Main: execute()
    
    Main->>Service: recommendFormats(log, options)
    
    Service->>Cache: 캐시 조회
    alt 캐시 히트
        Cache-->>Service: 캐시된 결과
        Service-->>User: 추천 결과 반환
    else 캐시 미스
        Service->>Service: 로그 포맷 로드
        Service->>Filter: 필터링 (그룹, 벤더 등)
        Filter-->>Service: 필터링된 포맷
        
        Service->>Matcher: matchLog(log, formats)
        
        loop 각 포맷에 대해
            Matcher->>Grok: compile(pattern)
            Grok-->>Matcher: Grok 객체
            Matcher->>Matcher: 패턴 매칭
            Matcher->>Matcher: 신뢰도 계산
        end
        
        Matcher-->>Service: 매칭 결과
        Service->>Service: 결과 정렬 및 필터링
        Service->>Cache: 결과 캐싱
        
        opt API 모드
            Service->>API: sendResults()
            API-->>Service: 응답
        end
        
        Service-->>User: 추천 결과 반환
    end
```

## 2. 상세 매칭 프로세스

```mermaid
sequenceDiagram
    participant Matcher as AdvancedLogMatcher
    participant Grok as GrokCompilerWrapper
    participant Pattern as GrokPattern
    participant Validator as FieldValidator
    participant Calculator as ConfidenceCalculator

    activate Matcher
    
    Matcher->>Matcher: preprocessLog(log)
    Note over Matcher: 로그 전처리 (트림, 정규화)
    
    Matcher->>Grok: loadCustomPatterns()
    Grok->>Grok: 표준 패턴 로드
    Grok->>Grok: 커스텀 패턴 로드 (200개)
    Grok-->>Matcher: 패턴 로드 완료
    
    loop 각 로그 포맷
        Matcher->>Grok: compile(grokExpression)
        alt 컴파일 캐시 존재
            Grok-->>Matcher: 캐시된 Grok 객체
        else 캐시 없음
            Grok->>Grok: synchronized 블록
            Grok->>Grok: PatternNormalizer.normalize()
            Grok->>Grok: compiler.compile()
            Grok->>Grok: 캐시 저장
            Grok-->>Matcher: 새 Grok 객체
        end
        
        Matcher->>Pattern: match(log)
        Pattern-->>Matcher: Match 객체
        
        Matcher->>Pattern: capture()
        Pattern-->>Matcher: 추출된 필드 Map
        
        alt 완전 매칭
            Matcher->>Calculator: calculateConfidence()
            Calculator->>Calculator: 필드 수 계산
            Calculator->>Calculator: log_time, message 제외
            Calculator->>Calculator: 특수 필드 가중치 적용
            Calculator->>Calculator: GREEDYDATA 패널티
            Calculator-->>Matcher: 신뢰도 (70-98%)
            
            Matcher->>Validator: validateFields()
            loop 각 필드
                Validator->>Validator: 타입별 검증
                Note over Validator: IP, Port, Timestamp 등
            end
            Validator-->>Matcher: 검증 결과
        else 부분 매칭
            Matcher->>Calculator: 부분 매칭 신뢰도
            Calculator-->>Matcher: 신뢰도 (최대 70%)
        end
    end
    
    Matcher->>Matcher: 결과 정렬 (신뢰도 기준)
    deactivate Matcher
```

## 3. 캐싱 메커니즘

```mermaid
sequenceDiagram
    participant Service as LogFormatRecommenderImpl
    participant PCM as PersistentCacheManager
    participant Memory as Caffeine캐시
    participant Disk as 디스크캐시
    participant File as 파일시스템

    Service->>PCM: getCachedResult(log)
    
    PCM->>PCM: generateCacheKey(log)
    Note over PCM: SHA-256 해시 생성
    
    PCM->>Memory: get(cacheKey)
    alt 메모리 캐시 히트
        Memory-->>PCM: 캐시된 결과
        PCM-->>Service: MatchResult
    else 메모리 캐시 미스
        PCM->>Disk: loadFromDisk(cacheKey)
        
        Disk->>File: ~/.logcenter/cache/results/{key}.cache
        alt 파일 존재 & 유효
            File-->>Disk: 캐시 데이터
            Disk->>Disk: TTL 체크 (24시간)
            alt TTL 유효
                Disk->>Disk: 역직렬화
                Disk-->>PCM: MatchResult
                PCM->>Memory: put(cacheKey, result)
                PCM-->>Service: MatchResult
            else TTL 만료
                Disk->>File: 캐시 파일 삭제
                Disk-->>PCM: null
                PCM-->>Service: null (캐시 미스)
            end
        else 파일 없음
            Disk-->>PCM: null
            PCM-->>Service: null (캐시 미스)
        end
    end
    
    Note over Service: 캐시 미스 시 매칭 수행
    
    Service->>Service: performMatching()
    Service->>PCM: cacheResult(log, result)
    
    PCM->>Memory: put(cacheKey, result)
    PCM->>Disk: saveToDisk(cacheKey, result)
    Disk->>Disk: 직렬화
    Disk->>File: ~/.logcenter/cache/results/{key}.cache
    
    PCM->>PCM: cleanupOldCache()
    Note over PCM: 30일 이상 된 캐시 삭제
```

## 4. 검증 도구 플로우

```mermaid
sequenceDiagram
    participant User as 사용자
    participant Main as ValidatorMain
    participant Validator as LogFormatValidator
    participant Grok as GrokCompilerWrapper
    participant Reporter as ValidationReportGenerator

    User->>Main: java -jar validator.jar
    Main->>Main: 옵션 파싱
    
    Main->>Validator: new LogFormatValidator()
    Validator->>Grok: new GrokCompilerWrapper()
    Validator->>Validator: loadCustomPatterns()
    
    Main->>Validator: validateAllFormats()
    
    Validator->>Validator: loadFormats(json)
    
    Note over Validator: 순차 처리 모드
    loop 각 포맷
        Validator->>Validator: validateFormat(format)
        
        loop 각 패턴
            Validator->>Validator: validatePattern()
            Validator->>Grok: compileSafe(pattern)
            
            alt 컴파일 성공
                Validator->>Validator: 샘플 로그 매칭
                Validator->>Validator: 필드 추출
                Validator->>Validator: 경고 체크
            else 컴파일 실패
                Validator->>Validator: 오류 기록
            end
            
            Validator->>Validator: ValidationResult 생성
        end
    end
    
    Validator-->>Main: List<ValidationResult>
    
    Main->>Reporter: generateReport(results, format)
    alt HTML 형식
        Reporter->>Reporter: generateHtmlReport()
    else JSON 형식
        Reporter->>Reporter: generateJsonReport()
    else Text 형식
        Reporter->>Reporter: generateTextReport()
    end
    
    Reporter-->>Main: 리포트 문자열
    
    alt 파일 출력
        Main->>Reporter: saveToFile(report, path)
        Reporter-->>User: 파일 저장 완료
    else 콘솔 출력
        Main-->>User: 리포트 출력
    end
```

## 5. API 클라이언트 플로우

```mermaid
sequenceDiagram
    participant Service as LogFormatRecommenderImpl
    participant Config as ApiConfiguration
    participant Client as DefaultApiClient
    participant HTTP as HttpURLConnection
    participant Server as API서버

    Service->>Config: isApiEnabled()
    alt API 모드 활성화
        Config-->>Service: true
        
        Service->>Client: new DefaultApiClient(config)
        Client->>Config: getApiUrl(), getTimeout()
        
        Service->>Client: sendMatchResult(result)
        
        Client->>Client: buildRequestBody()
        Note over Client: JSON 직렬화
        
        Client->>HTTP: openConnection(url)
        HTTP->>HTTP: setRequestMethod("POST")
        HTTP->>HTTP: setHeaders()
        HTTP->>HTTP: setTimeout(30초)
        
        Client->>HTTP: write(jsonBody)
        HTTP->>Server: POST /api/log-match
        
        Server-->>HTTP: 200 OK + Response
        HTTP-->>Client: ResponseBody
        
        Client->>Client: parseResponse()
        Client-->>Service: ApiResponse
        
        Service->>Service: 결과 병합/처리
    else API 모드 비활성화
        Config-->>Service: false
        Note over Service: API 호출 스킵
    end
```

## 주요 특징

### 1. 성능 최적화
- **다층 캐싱**: 메모리(Caffeine) + 영구 캐시(디스크)
- **패턴 컴파일 캐싱**: 한 번 컴파일된 패턴 재사용
- **순차 처리**: 병렬 처리 대신 순차 처리로 타임아웃 방지

### 2. 신뢰도 계산
- 완전 매칭: 70-98% 신뢰도
- 부분 매칭: 최대 70% 신뢰도
- 특수 필드 가중치 적용
- GREEDYDATA 패턴 패널티

### 3. 확장성
- 플러그인 아키텍처 (API 클라이언트)
- 다양한 출력 형식 지원 (JSON, HTML, Text)
- 필터링 옵션 (그룹, 벤더, 신뢰도)

### 4. 안정성
- 동기화된 패턴 컴파일
- 타임아웃 방지
- 오류 복구 메커니즘