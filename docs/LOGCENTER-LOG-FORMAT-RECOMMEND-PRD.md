# SIEM 로그포맷 추천 시스템 PRD (Product Requirements Document)

## 1. 제품 개요

### 1.1 프로젝트명
LogCenter Format Recommendation System (LFRS)

### 1.2 제품 설명
SIEM(Security Information and Event Management) 제품에서 사용자가 입력한 로그 샘플을 분석하여 가장 적합한 로그 포맷을 자동으로 추천하는 시스템입니다. Grok 패턴 매칭을 통해 GROK-PATTERN-CONVERTER.sql에 정의된 수백 개의 로그 포맷 중에서 최적의 포맷을 찾아 사용자의 로그 분석 설정 시간을 획기적으로 단축시킵니다.

### 1.3 핵심 가치
- **효율성**: 로그 포맷 선택 시간을 90% 이상 단축
- **정확성**: 95% 이상의 정확도로 올바른 로그 포맷 추천
- **확장성**: 다양한 로그 유형(보안, 시스템, 애플리케이션)에 대한 포괄적인 지원
- **사용성**: 직관적인 인터페이스와 상세한 매칭 정보 제공

## 2. 비즈니스 요구사항

### 2.1 배경 및 문제점
- **현재 상황**: SIEM 시스템에서 수백 개의 로그 포맷 중 적합한 포맷을 수동으로 찾아야 함
- **문제점**: 
  - 포맷 선택에 평균 30분 이상 소요
  - 잘못된 포맷 선택 시 로그 파싱 실패로 재작업 필요
  - 신규 로그 소스 추가 시 전문가 의존도 높음
  - 유사한 포맷들 간의 구분이 어려움

### 2.2 비즈니스 가치
- **운영 효율성**: 로그 설정 시간 단축으로 운영 비용 절감
- **정확성 향상**: 자동 추천으로 인한 오류 감소
- **전문가 의존도 감소**: 비전문가도 쉽게 로그 포맷 설정 가능
- **신속한 대응**: 새로운 로그 소스에 대한 빠른 대응

## 3. 대상 사용자

### 3.1 주요 사용자
- **SIEM 관리자**: 신규 로그 소스 설정 담당
- **보안 운영 센터(SOC) 분석가**: 로그 분석 및 모니터링
- **시스템 관리자**: 다양한 시스템 로그 관리
- **로그 분석 담당자**: 로그 파싱 규칙 설정

### 3.2 사용자 시나리오
1. **신규 로그 소스 추가**: 새로운 보안 장비나 시스템의 로그를 SIEM에 추가할 때
2. **로그 포맷 변경 대응**: 기존 시스템의 로그 포맷이 변경되었을 때
3. **문제 해결**: 로그 파싱 실패 시 올바른 포맷 찾기
4. **포맷 검증**: 선택한 포맷이 맞는지 확인

## 4. 기능 요구사항

### 4.1 핵심 기능

#### 4.1.1 로그 샘플 입력
- **직접 입력**: 텍스트 박스를 통한 로그 샘플 직접 입력
  - 단일 라인 입력
  - 멀티라인 입력 (최대 100줄)
- **파일 업로드**: 로그 파일 업로드
  - 지원 형식: .txt, .log, .csv
  - 최대 파일 크기: 10MB
  - 인코딩 자동 감지 (UTF-8, EUC-KR, ASCII)

#### 4.1.2 로그 포맷 분석
- **Grok 패턴 매칭**
  - GROK-PATTERN-CONVERTER.sql의 모든 패턴과 비교
  - 부분 매칭 및 완전 매칭 지원
  - 매칭 점수 계산 알고리즘
- **필드 추출**
  - 매칭된 필드 자동 추출
  - 필드명과 값 매핑
  - 필드 타입 자동 인식

#### 4.1.3 추천 결과 제공
- **추천 목록**
  - 상위 10개 매칭 포맷 표시
  - 매칭률(%) 표시
  - 신뢰도 점수 제공
- **상세 정보**
  - 선택된 포맷의 Grok 패턴
  - 파싱된 필드 미리보기
  - 샘플 로그와의 비교 시각화

#### 4.1.4 포맷 선택 및 적용
- **포맷 선택**
  - 추천 목록에서 선택
  - 선택한 포맷의 검증
- **테스트 기능**
  - 추가 로그 샘플로 검증
  - 파싱 결과 미리보기

### 4.2 부가 기능

#### 4.2.1 학습 및 개선
- **피드백 수집**
  - 추천 정확도에 대한 사용자 피드백
  - 잘못된 추천 리포트
- **패턴 최적화**
  - 자주 사용되는 패턴 우선순위 조정
  - 새로운 패턴 제안

#### 4.2.2 관리 기능
- **포맷 관리**
  - 새로운 Grok 패턴 추가
  - 기존 패턴 수정/삭제
  - 패턴 버전 관리
- **통계 및 리포트**
  - 추천 사용 통계
  - 정확도 리포트
  - 성능 모니터링

## 5. 기술 요구사항

### 5.1 개발 환경
- **프로그래밍 언어**: Java
- **JDK 버전**: 1.8 이하 호환성 필수
- **빌드 도구**: Maven 3.6+

### 5.2 핵심 라이브러리 및 프레임워크
- **Grok 파싱**: io.krakens:java-grok (0.1.9) - JDK 1.8 호환
- **JSON 처리**: com.google.code.gson:gson (2.8.9) - JDK 1.8 호환
- **로깅**: 
  - slf4j-api (1.7.25)
  - logback-classic (1.2.3)
- **테스트**: 
  - junit (4.12)
  - mockito-core (2.23.4)
- **유틸리티**: 
  - commons-lang3 (3.8.1)
  - commons-io (2.6)
- **CLI 프레임워크**: picocli (4.6.1)
- **HTTP 클라이언트**: Apache HttpClient (4.5.13)

### 5.3 Maven 프로젝트 구조

#### 5.3.1 프로젝트 구조 (Maven Standard Directory Layout)
```
logcenter-format-recommender/
├── pom.xml                                      # Maven 프로젝트 설정
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── logcenter/
│   │   │           └── recommender/
│   │   │               ├── Main.java                    # 메인 진입점
│   │   │               ├── config/
│   │   │               │   ├── AppConfig.java          # 설정 관리
│   │   │               │   └── ConfigLoader.java       # 설정 로더
│   │   │               ├── model/
│   │   │               │   ├── LogFormat.java          # 로그 포맷 엔티티
│   │   │               │   ├── GrokPattern.java        # Grok 패턴 엔티티
│   │   │               │   ├── MatchResult.java        # 매칭 결과
│   │   │               │   └── FormatRecommendation.java # 추천 결과
│   │   │               ├── service/
│   │   │               │   ├── FormatLoader.java       # SQL 파일 로더
│   │   │               │   ├── GrokMatcher.java        # Grok 매칭 엔진
│   │   │               │   ├── Recommender.java        # 추천 서비스
│   │   │               │   └── FieldValidator.java     # 필드 검증기
│   │   │               ├── util/
│   │   │               │   ├── GrokCompiler.java       # Grok 컴파일러
│   │   │               │   ├── LogParser.java          # 로그 파서
│   │   │               │   └── JsonUtils.java          # JSON 유틸리티
│   │   │               └── cli/
│   │   │                   ├── CliCommand.java         # CLI 명령어
│   │   │                   └── OutputFormatter.java    # 출력 포맷터
│   │   └── resources/
│   │       ├── application.properties           # 애플리케이션 설정
│   │       ├── logback.xml                     # 로깅 설정
│   │       ├── grok-patterns/                  # 기본 Grok 패턴
│   │       │   └── patterns                    # 표준 패턴 파일
│   │       └── custom-grok-patterns            # 커스텀 패턴 파일
│   └── test/
│       ├── java/
│       │   └── com/
│       │       └── logcenter/
│       │           └── recommender/
│       │               ├── service/
│       │               │   ├── GrokMatcherTest.java
│       │               │   ├── RecommenderTest.java
│       │               │   └── FormatLoaderTest.java
│       │               ├── util/
│       │               │   └── GrokCompilerTest.java
│       │               └── integration/
│       │                   └── EndToEndTest.java
│       └── resources/
│           ├── test-logs/                      # 테스트용 로그 샘플
│           │   ├── apache-access.log
│           │   ├── firewall.log
│           │   └── system.log
│           └── test-formats.json               # 테스트용 포맷 데이터
└── target/                                     # Maven 빌드 출력 디렉토리
    ├── classes/
    ├── test-classes/
    ├── logcenter-format-recommender-1.0.0.jar
    └── logcenter-format-recommender-1.0.0-jar-with-dependencies.jar
```

#### 5.3.2 Maven POM 설정 (pom.xml)
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    
    <modelVersion>4.0.0</modelVersion>
    
    <groupId>com.logcenter</groupId>
    <artifactId>logcenter-format-recommender</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>
    
    <name>LogCenter Format Recommender</name>
    <description>SIEM Log Format Recommendation System using Grok Patterns</description>
    
    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
        <maven.compiler.source>1.8</maven.compiler.source>
        <maven.compiler.target>1.8</maven.compiler.target>
        <java.version>1.8</java.version>
        
        <!-- 라이브러리 버전 관리 -->
        <grok.version>0.1.9</grok.version>
        <gson.version>2.8.9</gson.version>
        <slf4j.version>1.7.25</slf4j.version>
        <logback.version>1.2.3</logback.version>
        <junit.version>4.12</junit.version>
        <mockito.version>2.23.4</mockito.version>
        <commons-lang3.version>3.8.1</commons-lang3.version>
        <commons-io.version>2.6</commons-io.version>
        <picocli.version>4.6.1</picocli.version>
        <httpclient.version>4.5.13</httpclient.version>
    </properties>
    
    <dependencies>
        <!-- Grok 라이브러리 (JDK 1.8 호환) -->
        <dependency>
            <groupId>io.krakens</groupId>
            <artifactId>java-grok</artifactId>
            <version>${grok.version}</version>
        </dependency>
        
        <!-- JSON 처리 -->
        <dependency>
            <groupId>com.google.code.gson</groupId>
            <artifactId>gson</artifactId>
            <version>${gson.version}</version>
        </dependency>
        
        <!-- 로깅 -->
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
            <version>${slf4j.version}</version>
        </dependency>
        <dependency>
            <groupId>ch.qos.logback</groupId>
            <artifactId>logback-classic</artifactId>
            <version>${logback.version}</version>
        </dependency>
        
        <!-- 유틸리티 -->
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-lang3</artifactId>
            <version>${commons-lang3.version}</version>
        </dependency>
        <dependency>
            <groupId>commons-io</groupId>
            <artifactId>commons-io</artifactId>
            <version>${commons-io.version}</version>
        </dependency>
        
        <!-- CLI 프레임워크 -->
        <dependency>
            <groupId>info.picocli</groupId>
            <artifactId>picocli</artifactId>
            <version>${picocli.version}</version>
        </dependency>
        
        <!-- HTTP 클라이언트 (API 통신용) -->
        <dependency>
            <groupId>org.apache.httpcomponents</groupId>
            <artifactId>httpclient</artifactId>
            <version>${httpclient.version}</version>
        </dependency>
        
        <!-- 테스트 의존성 -->
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <version>${junit.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-core</artifactId>
            <version>${mockito.version}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
    
    <build>
        <finalName>${project.artifactId}-${project.version}</finalName>
        
        <plugins>
            <!-- Maven Compiler Plugin -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.8.1</version>
                <configuration>
                    <source>1.8</source>
                    <target>1.8</target>
                    <encoding>UTF-8</encoding>
                </configuration>
            </plugin>
            
            <!-- Maven Resources Plugin -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-resources-plugin</artifactId>
                <version>3.2.0</version>
                <configuration>
                    <encoding>UTF-8</encoding>
                </configuration>
            </plugin>
            
            <!-- Maven JAR Plugin -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-jar-plugin</artifactId>
                <version>3.2.0</version>
                <configuration>
                    <archive>
                        <manifest>
                            <addClasspath>true</addClasspath>
                            <classpathPrefix>lib/</classpathPrefix>
                            <mainClass>com.logcenter.recommender.Main</mainClass>
                        </manifest>
                    </archive>
                </configuration>
            </plugin>
            
            <!-- Maven Assembly Plugin (실행 가능한 JAR 생성) -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-assembly-plugin</artifactId>
                <version>3.3.0</version>
                <configuration>
                    <descriptorRefs>
                        <descriptorRef>jar-with-dependencies</descriptorRef>
                    </descriptorRefs>
                    <archive>
                        <manifest>
                            <mainClass>com.logcenter.recommender.Main</mainClass>
                        </manifest>
                    </archive>
                </configuration>
                <executions>
                    <execution>
                        <id>make-assembly</id>
                        <phase>package</phase>
                        <goals>
                            <goal>single</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
            
            <!-- Maven Surefire Plugin (테스트) -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>2.22.2</version>
                <configuration>
                    <includes>
                        <include>**/*Test.java</include>
                    </includes>
                    <excludes>
                        <exclude>**/integration/*Test.java</exclude>
                    </excludes>
                </configuration>
            </plugin>
            
            <!-- Maven Failsafe Plugin (통합 테스트) -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-failsafe-plugin</artifactId>
                <version>2.22.2</version>
                <configuration>
                    <includes>
                        <include>**/integration/*Test.java</include>
                    </includes>
                </configuration>
                <executions>
                    <execution>
                        <goals>
                            <goal>integration-test</goal>
                            <goal>verify</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
            
            <!-- Maven Clean Plugin -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-clean-plugin</artifactId>
                <version>3.1.0</version>
            </plugin>
        </plugins>
    </build>
    
    <profiles>
        <!-- 개발 프로파일 -->
        <profile>
            <id>dev</id>
            <properties>
                <log.level>DEBUG</log.level>
            </properties>
        </profile>
        
        <!-- 운영 프로파일 -->
        <profile>
            <id>prod</id>
            <properties>
                <log.level>INFO</log.level>
            </properties>
        </profile>
    </profiles>
</project>
```

#### 5.3.3 빌드 및 실행 명령어
```bash
# 프로젝트 빌드
mvn clean compile

# 테스트 실행
mvn test

# 통합 테스트 포함 전체 테스트
mvn verify

# JAR 패키징 (의존성 포함)
mvn clean package

# 특정 프로파일로 빌드
mvn clean package -Pdev  # 개발 환경
mvn clean package -Pprod # 운영 환경

# 애플리케이션 실행
java -jar target/logcenter-format-recommender-1.0.0-jar-with-dependencies.jar

# 메모리 설정과 함께 실행
java -Xmx2G -Xms512M -jar target/logcenter-format-recommender-1.0.0-jar-with-dependencies.jar

# 로깅 레벨 설정하여 실행
java -Dlogback.configurationFile=logback.xml -jar target/logcenter-format-recommender-1.0.0-jar-with-dependencies.jar
```

#### 5.3.4 Maven 빌드 라이프사이클
```bash
# 클린 빌드
mvn clean install

# 빠른 빌드 (테스트 스킵)
mvn clean package -DskipTests

# 의존성 트리 확인
mvn dependency:tree

# 의존성 분석 (사용하지 않는 의존성 확인)
mvn dependency:analyze

# 프로젝트 정보 확인
mvn help:effective-pom

# JavaDoc 생성
mvn javadoc:javadoc
```

### 5.4 시스템 아키텍처

#### 5.4.1 컴포넌트 구조
```
┌─────────────────────────────────────────────────┐
│                  사용자 인터페이스                  │
│                (CLI / Web UI / API)               │
├─────────────────────────────────────────────────┤
│             Format Recommendation Service        │
├──────────────────┬────────────────┬─────────────┤
│   Input Parser   │ Pattern Matcher│ Result Ranker│
├──────────────────┴────────────────┴─────────────┤
│               Grok Pattern Engine                │
├─────────────────────────────────────────────────┤
│        Pattern Repository (SQL/Memory)           │
└─────────────────────────────────────────────────┘
```

#### 5.4.2 주요 클래스 설계
```java
// 핵심 인터페이스
public interface LogFormatRecommender {
    List<FormatRecommendation> recommend(String logSample);
    List<FormatRecommendation> recommend(File logFile);
    List<FormatRecommendation> recommend(String logSample, RecommendOptions options);
}

// 추천 결과 모델
public class FormatRecommendation {
    private String formatId;        // 포맷 고유 ID
    private String formatName;      // 포맷 이름
    private String groupName;       // 그룹 (Firewall, Web Server 등)
    private String grokPattern;     // Grok 패턴
    private double matchScore;      // 매칭 점수 (0-100)
    private double confidence;      // 신뢰도 (0-100)
    private Map<String, String> extractedFields;  // 추출된 필드
    private String matchedPattern;  // 매칭된 패턴 이름
}

// Grok 패턴 매처
public interface GrokMatcher {
    MatchResult match(String log, String pattern);
    List<MatchResult> matchAll(String log, List<String> patterns);
}

// 매칭 옵션
public class RecommendOptions {
    private int maxResults = 10;
    private double minConfidence = 70.0;
    private boolean includeFieldExtraction = true;
    private String groupFilter;  // 특정 그룹으로 필터링
}
```

### 5.5 성능 요구사항
- **응답 시간**: 
  - 단일 로그 라인: < 100ms
  - 100줄 로그 파일: < 5초
- **동시 처리**: 최소 100개 동시 요청 처리
- **메모리 사용**: 최대 2GB Heap Memory
- **패턴 로딩**: 애플리케이션 시작 시 < 10초

### 5.6 보안 요구사항
- **입력 검증**: 
  - SQL Injection 방지
  - 파일 업로드 악성코드 스캔
  - 입력 크기 제한
- **접근 제어**: 
  - API 인증 토큰
  - Rate Limiting
- **로그 보안**: 
  - 민감 정보 마스킹
  - 로그 저장 시 암호화

## 6. API 인터페이스 설계

### 6.1 REST API 엔드포인트

#### 6.1.1 로그 포맷 추천
```
POST /api/v1/recommend/format
Content-Type: application/json

Request Body:
{
    "logSample": "2024-01-15 10:30:45 192.168.1.100 GET /api/users 200 125ms",
    "options": {
        "maxResults": 10,
        "minConfidence": 0.7,
        "includeDetails": true
    }
}

Response:
{
    "recommendations": [
        {
            "formatId": "APACHE_ACCESS_2.0_1",
            "formatName": "Apache Access Log",
            "groupName": "Web Server",
            "matchScore": 0.95,
            "confidence": 0.92,
            "grokPattern": "%{TIMESTAMP_ISO8601:timestamp} %{IP:client_ip}...",
            "extractedFields": {
                "timestamp": "2024-01-15 10:30:45",
                "client_ip": "192.168.1.100",
                "method": "GET",
                "uri": "/api/users",
                "status": "200",
                "response_time": "125ms"
            }
        }
    ],
    "processingTime": 85,
    "status": "success"
}
```

#### 6.1.2 파일 업로드 추천
```
POST /api/v1/recommend/file
Content-Type: multipart/form-data

Request:
- file: (binary)
- options: {"maxResults": 5}

Response: (동일한 형식)
```

### 6.2 Java SDK 인터페이스
```java
// SDK 사용 예제
LogFormatRecommender recommender = new LogFormatRecommenderImpl();

// 단일 로그 추천
String logSample = "2024-01-15 10:30:45 ERROR [main] com.example.App - Connection timeout";
List<FormatRecommendation> recommendations = recommender.recommend(logSample);

// 파일 기반 추천
File logFile = new File("/path/to/sample.log");
List<FormatRecommendation> fileRecommendations = recommender.recommend(logFile);

// 옵션 설정
RecommendOptions options = RecommendOptions.builder()
    .maxResults(5)
    .minConfidence(0.8)
    .includeFieldExtraction(true)
    .groupFilter("Firewall")
    .build();
    
List<FormatRecommendation> customRecommendations = 
    recommender.recommend(logSample, options);
```

## 7. 데이터 모델

### 7.1 로그 포맷 스키마 (GROK-PATTERN-CONVERTER.sql)
```json
{
    "sm_type": "logformat6",
    "log_type": [
        {
            "type_name": "Apache Access Log",
            "patterns": [
                {
                    "exp_name": "APACHE_ACCESS_2.0_1",
                    "grok_exp": "%{COMMONAPACHELOG}",
                    "samplelog": "127.0.0.1 - - [10/Oct/2000:13:55:36 -0700]...",
                    "order": "1"
                }
            ]
        }
    ]
}
```

### 7.2 로그 포맷 추천 대상 상세 정보

#### 7.2.1 전체 로그 포맷 현황
GROK-PATTERN-CONVERTER.sql에 정의된 전체 100개의 로그 포맷이 추천 대상입니다.

**그룹별 분포:**
- **Firewall (23개)**: 방화벽 및 통합보안 장비
- **Web Server (4개)**: 웹 서버 액세스 로그
- **System (11개)**: 운영체제 시스템 로그
- **Web Firewall (14개)**: 웹 방화벽 및 WAF
- **IPS (8개)**: 침입방지시스템
- **Application (18개)**: 애플리케이션 로그
- **SWITCH (6개)**: 네트워크 스위치
- **DDOS (4개)**: DDoS 방어 솔루션
- **VPN (4개)**: VPN 장비
- **APT (3개)**: 지능형 위협 대응
- **NAC (2개)**: 네트워크 접근 제어
- **Beats (1개)**: Elastic Beats
- **기타 (2개)**: M365, 명령어 로그

#### 7.2.2 주요 벤더별 로그 포맷

**1. Firewall 그룹 (23개)**
- **JUNIPER_NETWORKS_NETSCREEN**: Juniper NetScreen 시리즈
- **NEXG_SECUREWORKS**: 넥스지 SecureWorks
- **CHECKPOINT_NGFW/QUANTUM**: Check Point 차세대 방화벽
- **SECUI_MF2/BLUEMAX_NGF/NXG**: 시큐아이 통합보안장비
- **FORTINET_FG600C**: Fortinet FortiGate 시리즈
- **AHNLAB_TRUSGUARD**: 안랩 TrusGuard
- **CISCO_ASA/FIREPOWER**: Cisco 보안 장비
- **PALOALTONETWORKS_PALOALTO**: Palo Alto Networks
- **JUNIPER_NETWORKS_SRX**: Juniper SRX 시리즈
- **AXGATE_AXGATE**: 액스게이트
- **HILLSTONE_NETWORKS_NGFW**: Hillstone 차세대 방화벽

**2. Web Server 그룹 (4개)**
- **APACHE_HTTP**: Apache HTTP Server
- **TMAXSOFT_JEUS**: 티맥스소프트 JEUS
- **TMAXSOFT_WEBTOB**: 티맥스소프트 WebToB
- **NGINX_NGINX**: Nginx 웹 서버

**3. System 그룹 (11개)**
- **LINUX_CENTOS**: Linux CentOS
- **HP_UX**: HP-UX
- **IBM_AIX**: IBM AIX
- **ORACLE_SOLARIS**: Oracle Solaris
- **LINUX_AUDITD**: Linux Audit Daemon
- **SYSTEM_SYSTEM**: 일반 시스템 로그
- **LINUX_DHCP**: Linux DHCP
- **LINUX_COMMAND**: Linux 명령어 로그

**4. Web Firewall 그룹 (14개)**
- **PIOLINK_WEBFRONT**: 파이오링크 WEBFRONT
- **WINS_SNIPER_WAF**: 윈스 Sniper WAF
- **MONITORAPP_AIWAF**: 모니터앱 AIWAF (3개 버전)
- **PENTASECURITY_WAPPLE**: 펜타시큐리티 WAPPLES
- **TRINITYSOFT_WEBS_RAY**: 트리니티소프트 Webs Ray
- **F5_AWAF**: F5 Advanced WAF

**5. IPS 그룹 (8개)**
- **TREND_MICRO_TIPPINGPOINT**: Trend Micro TippingPoint
- **WINS_SNIPER_IPS**: 윈스 Sniper IPS
- **SECUI_MFI**: 시큐아이 MFI
- **WINS_SNIPER_ONE**: 윈스 Sniper ONE

**6. Application 그룹 (18개)**
- **데이터베이스**: MONGODB, POSTGRESQL, MYSQL, MSSQL
- **메시지 큐**: RABBITMQ, REDIS, ACTIVEMQ
- **모니터링**: ICINGA, ZOOKEEPER
- **웹 서버**: NGINX, TRAEFIK, HAPROXY
- **검색엔진**: ELASTICSEARCH
- **로그 처리**: LOGSTASH

**7. 기타 그룹**
- **SWITCH (6개)**: 3COM, EXTREME, JUNIPER, NORTEL, PIOLINK, CISCO
- **DDOS (4개)**: SECUI_MFD, RADWARE_DEFENSEPRO, AHNLAB_DPX
- **VPN (4개)**: SSL_VPN, NEXG_VPN, SECUWAY_SSL_U
- **APT (3개)**: FIREEYE_MPS, SYMANTEC_SEP, TRENDMICRO_DEEP_DISCOVERY
- **NAC (2개)**: GENIAN_NAC, NETMAN_SMART_NAC

#### 7.2.3 패턴 복잡도별 분류

**고복잡도 패턴 (필드 10개 이상)**
- Firewall 그룹의 대부분 패턴
- IPS 그룹의 상세 이벤트 패턴
- APT 솔루션의 위협 탐지 패턴

**중복잡도 패턴 (필드 5-9개)**
- Web Server 액세스 로그
- System 이벤트 로그
- Application 로그

**저복잡도 패턴 (필드 5개 미만)**
- 간단한 시스템 메시지
- 기본 이벤트 로그

### 7.3 매칭 알고리즘

#### 7.3.1 매칭 점수 계산
```
MatchScore = (FieldMatchCount / TotalFieldCount) * FieldWeight + 
             (PatternCoverage / LogLength) * CoverageWeight +
             (DataTypeAccuracy) * TypeWeight

Where:
- FieldWeight = 0.5
- CoverageWeight = 0.3
- TypeWeight = 0.2
```

#### 7.3.2 신뢰도 계산
```
Confidence = MatchScore * FrequencyFactor * ComplexityFactor

Where:
- FrequencyFactor: 해당 포맷의 사용 빈도
- ComplexityFactor: 패턴의 복잡도 (더 구체적인 패턴일수록 높은 점수)
```

## 8. 기술 구현

### 8.1 기술 스택
- **언어**: Java 8 (JDK 1.8)
- **빌드**: Maven 3.6+
- **의존성 관리**: Maven Central Repository
- **패키징**: 
  - 표준 JAR (의존성 별도)
  - Fat JAR with dependencies (단독 실행 가능)
- **주요 의존성**:
  - io.krakens:java-grok (Grok 파서)
  - com.google.code.gson:gson (JSON 처리)
  - info.picocli:picocli (CLI 프레임워크)
  - org.apache.httpcomponents:httpclient (API 통신)
  - org.apache.commons:commons-lang3 (유틸리티)

### 8.2 Maven 프로젝트 설정

#### 8.2.1 프로젝트 생성
```bash
# Maven 프로젝트 생성
mvn archetype:generate \
    -DgroupId=com.logcenter \
    -DartifactId=logcenter-format-recommender \
    -DarchetypeArtifactId=maven-archetype-quickstart \
    -DarchetypeVersion=1.4 \
    -DinteractiveMode=false

# 프로젝트 디렉토리로 이동
cd logcenter-format-recommender
```

#### 8.2.2 디렉토리 구조 설정
```bash
# 필요한 디렉토리 생성
mkdir -p src/main/java/com/logcenter/recommender/{config,model,service,util,cli}
mkdir -p src/main/resources/grok-patterns
mkdir -p src/test/java/com/logcenter/recommender/{service,util,integration}
mkdir -p src/test/resources/test-logs

# 리소스 파일 복사
cp /path/to/custom-grok-patterns src/main/resources/
cp /path/to/GROK-PATTERN-CONVERTER.sql src/main/resources/
```

#### 8.2.3 IDE 설정
```bash
# IntelliJ IDEA 프로젝트 파일 생성
mvn idea:idea

# Eclipse 프로젝트 파일 생성
mvn eclipse:eclipse

# VS Code 설정 (Java Extension Pack 필요)
# .vscode/settings.json 파일 생성
```

### 8.3 매칭 알고리즘 상세

#### 8.3.1 매칭 알고리즘 개요 (단순화된 버전)

로그 포맷 매칭 알고리즘은 **Grok 패턴 완전 매칭을 최우선**으로 하여 빠르고 정확한 결과를 제공합니다. 

**핵심 원칙:**
1. Grok 패턴이 완전 매칭되면 그것이 정답일 가능성이 매우 높음
2. 단일 완전 매칭 시 즉시 95%+ 신뢰도 부여
3. 다중 완전 매칭 시에만 추가 평가 수행
4. 완전 매칭이 없을 때만 부분 매칭 고려

**프로세스 흐름:**
```
로그 입력 → Grok 완전 매칭 시도
    ├─ 단일 매칭 → 즉시 반환 (95-100%)
    ├─ 다중 매칭 → 우선순위 평가
    └─ 매칭 없음 → 부분 매칭 시도
```

#### 8.3.2 단순화된 매칭 프로세스

##### Step 1: Grok 완전 매칭 우선
```java
public class SimplifiedLogMatcher {
    
    public List<FormatRecommendation> match(String log) {
        // 1. 모든 포맷에 대해 Grok 매칭 시도
        List<MatchResult> allMatches = tryGrokMatching(log);
        
        // 2. 완전 매칭 필터링
        List<MatchResult> perfectMatches = allMatches.stream()
            .filter(m -> m.isCompleteMatch())
            .collect(Collectors.toList());
        
        // 3. 결과 처리
        if (perfectMatches.size() == 1) {
            // 단일 완전 매칭 → 즉시 반환
            return Arrays.asList(
                createRecommendation(perfectMatches.get(0), 98.0)
            );
        } else if (perfectMatches.size() > 1) {
            // 다중 완전 매칭 → 우선순위 평가
            return evaluateMultipleMatches(perfectMatches);
        } else {
            // 완전 매칭 없음 → 부분 매칭 평가
            return evaluatePartialMatches(allMatches);
        }
    }
    
    // Grok 매칭 수행
    private List<MatchResult> tryGrokMatching(String log) {
        List<MatchResult> results = new ArrayList<>();
        
        // 병렬 처리로 성능 향상
        getAllFormats().parallelStream().forEach(format -> {
            for (Pattern pattern : format.getPatterns()) {
                try {
                    Grok grok = new Grok();
                    grok.compile(pattern.getGrokExpression());
                    Match match = grok.match(log);
                    match.captures();
                    
                    if (!match.isNull()) {
                        MatchResult result = new MatchResult();
                        result.format = format;
                        result.pattern = pattern;
                        result.extractedFields = match.toMap();
                        result.isCompleteMatch = isCompleteMatch(log, match);
                        results.add(result);
                    }
                } catch (Exception e) {
                    // 패턴 오류는 무시
                }
            }
        });
        
        return results;
    }
    
    // 완전 매칭 여부 판단
    private boolean isCompleteMatch(String log, Match match) {
        // 전체 로그가 패턴과 매칭되었는지 확인
        String matched = match.getSubject();
        return matched != null && matched.trim().equals(log.trim());
    }
}
```

```java
public class MultipleMatchEvaluator {
    
    // 다중 완전 매칭 시 우선순위 평가
    private List<FormatRecommendation> evaluateMultipleMatches(
            List<MatchResult> perfectMatches) {
        
        return perfectMatches.stream()
            .map(match -> {
                // 기본 점수: 완전 매칭이므로 95점 시작
                double score = 95.0;
                
                // 1. 필드 추출 개수 (많을수록 좋음)
                int fieldCount = match.extractedFields.size();
                if (fieldCount >= 10) score += 2.0;
                else if (fieldCount >= 5) score += 1.0;
                
                // 2. GREEDYDATA 패널티
                long greedyCount = countGreedyDataFields(match);
                score -= greedyCount * 2.0;
                
                // 3. 패턴 구체성 (패턴 길이)
                int patternLength = match.pattern.getGrokExpression().length();
                if (patternLength > 200) score += 1.0;
                
                // 4. 그룹별 특화 보너스 (간단히)
                score += getGroupBonus(match);
                
                return createRecommendation(match, Math.min(100, score));
            })
            .sorted((a, b) -> Double.compare(b.confidence, a.confidence))
            .collect(Collectors.toList());
    }
    
    // GREEDYDATA 필드 개수 계산
    private long countGreedyDataFields(MatchResult match) {
        String pattern = match.pattern.getGrokExpression();
        return Arrays.stream(pattern.split("%"))
            .filter(s -> s.contains("GREEDYDATA"))
            .count();
    }
    
    // 그룹별 간단한 보너스
    private double getGroupBonus(MatchResult match) {
        String groupName = match.format.getGroupName();
        String log = match.originalLog;
        
        switch (groupName) {
            case "Firewall":
                // IP 쌍과 액션 키워드 확인
                if (hasField(match, "src_ip") && hasField(match, "dst_ip") &&
                    log.matches(".*(ACCEPT|DROP|DENY).*")) {
                    return 2.0;
                }
                break;
                
            case "Web Server":
                // HTTP 메소드와 상태 코드 확인
                if (hasField(match, "method") && hasField(match, "status")) {
                    return 2.0;
                }
                break;
        }
        
        return 0.0;
    }
}
```

##### Step 3: 부분 매칭 처리 (완전 매칭 실패 시)
```java
public class PartialMatchEvaluator {
    
    // 부분 매칭 평가 (완전 매칭이 없을 때만 사용)
    private List<FormatRecommendation> evaluatePartialMatches(
            List<MatchResult> allMatches) {
        
        if (allMatches.isEmpty()) {
            return Collections.emptyList();
        }
        
        return allMatches.stream()
            .filter(m -> !m.isCompleteMatch)
            .map(match -> {
                // 부분 매칭은 최대 70점
                double score = 0.0;
                
                // 1. 매칭된 필드 수 (최대 40점)
                int fieldCount = match.extractedFields.size();
                score += Math.min(40, fieldCount * 5);
                
                // 2. 중요 필드 존재 여부 (최대 20점)
                if (hasTimestamp(match)) score += 10;
                if (hasIPAddress(match)) score += 5;
                if (hasLogLevel(match)) score += 5;
                
                // 3. 커버리지 (최대 10점)
                double coverage = calculateCoverage(match);
                score += coverage * 10;
                
                return createRecommendation(match, score);
            })
            .filter(rec -> rec.confidence >= 30) // 30% 미만은 제외
            .sorted((a, b) -> Double.compare(b.confidence, a.confidence))
            .limit(5) // 상위 5개만
            .collect(Collectors.toList());
    }
    
    // 타임스탬프 필드 확인
    private boolean hasTimestamp(MatchResult match) {
        return match.extractedFields.keySet().stream()
            .anyMatch(field -> field.toLowerCase().contains("time") || 
                              field.toLowerCase().contains("date"));
    }
    
    // IP 주소 필드 확인
    private boolean hasIPAddress(MatchResult match) {
        return match.extractedFields.keySet().stream()
            .anyMatch(field -> field.toLowerCase().contains("ip") || 
                              field.toLowerCase().contains("addr"));
    }
    
    // 로그 레벨 필드 확인
    private boolean hasLogLevel(MatchResult match) {
        return match.extractedFields.keySet().stream()
            .anyMatch(field -> field.toLowerCase().contains("level") || 
                              field.toLowerCase().contains("severity"));
    }
    
    // 간단한 커버리지 계산
    private double calculateCoverage(MatchResult match) {
        int totalFieldsLength = match.extractedFields.values().stream()
            .mapToInt(String::length)
            .sum();
        return Math.min(1.0, (double) totalFieldsLength / match.originalLog.length());
    }
}
```

#### 8.3.3 신뢰도 계산 개요

**단순화된 신뢰도 계산:**

1. **완전 매칭**
   - 단일 매칭: 98% 고정
   - 다중 매칭: 95% + 보너스/패널티

2. **부분 매칭**
   - 최대 70%
   - 필드 수, 중요 필드, 커버리지 기반

3. **평가 기준**
   - 필드 추출 개수 (많을수록 좋음)
   - GREEDYDATA 사용 최소화
   - 패턴 구체성
   - 그룹별 특화 키워드

#### 8.3.3 필드 검증기 (Field Validators)

각 필드 타입에 대한 상세한 검증 로직:

```java
public class FieldValidators {
    
    // IP 주소 검증기
    public static class IPValidator implements FieldValidator {
        @Override
        public ValidationResult validate(String value) {
            ValidationResult result = new ValidationResult();
            
            // IPv4 검증
            if (value.matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$")) {
                String[] parts = value.split("\\.");
                boolean valid = true;
                
                for (String part : parts) {
                    int num = Integer.parseInt(part);
                    if (num < 0 || num > 255) {
                        valid = false;
                        break;
                    }
                }
                
                result.valid = valid;
                result.type = "IPv4";
                result.confidence = valid ? 1.0 : 0.0;
                
            // IPv6 검증
            } else if (value.matches("^[0-9a-fA-F:]+$")) {
                result.valid = isValidIPv6(value);
                result.type = "IPv6";
                result.confidence = result.valid ? 0.9 : 0.0;
            } else {
                result.valid = false;
                result.confidence = 0.0;
            }
            
            return result;
        }
    }
    
    // 타임스탬프 검증기
    public static class TimestampValidator implements FieldValidator {
        private static final List<DateTimeFormatter> FORMATTERS = Arrays.asList(
            DateTimeFormatter.ISO_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z"),
            DateTimeFormatter.ofPattern("MMM dd HH:mm:ss")
        );
        
        @Override
        public ValidationResult validate(String value) {
            ValidationResult result = new ValidationResult();
            
            for (DateTimeFormatter formatter : FORMATTERS) {
                try {
                    formatter.parse(value);
                    result.valid = true;
                    result.type = "timestamp";
                    result.confidence = 1.0;
                    result.metadata.put("format", formatter.toString());
                    return result;
                } catch (Exception e) {
                    // 다음 포맷 시도
                }
            }
            
            // Unix timestamp 체크
            if (value.matches("^\\d{10}$")) {
                result.valid = true;
                result.type = "unix_timestamp";
                result.confidence = 0.8;
                return result;
            }
            
            result.valid = false;
            result.confidence = 0.0;
            return result;
        }
    }
    
    // HTTP 상태 코드 검증기
    public static class HTTPStatusValidator implements FieldValidator {
        @Override
        public ValidationResult validate(String value) {
            ValidationResult result = new ValidationResult();
            
            try {
                int status = Integer.parseInt(value);
                result.valid = status >= 100 && status <= 599;
                result.type = "http_status";
                
                if (result.valid) {
                    result.confidence = 1.0;
                    // 상태 코드 분류
                    if (status >= 100 && status < 200) {
                        result.metadata.put("category", "informational");
                    } else if (status >= 200 && status < 300) {
                        result.metadata.put("category", "success");
                    } else if (status >= 300 && status < 400) {
                        result.metadata.put("category", "redirect");
                    } else if (status >= 400 && status < 500) {
                        result.metadata.put("category", "client_error");
                    } else {
                        result.metadata.put("category", "server_error");
                    }
                }
            } catch (NumberFormatException e) {
                result.valid = false;
                result.confidence = 0.0;
            }
            
            return result;
        }
    }
    
    // 포트 번호 검증기
    public static class PortValidator implements FieldValidator {
        @Override
        public ValidationResult validate(String value) {
            ValidationResult result = new ValidationResult();
            
            try {
                int port = Integer.parseInt(value);
                result.valid = port >= 1 && port <= 65535;
                result.type = "port";
                
                if (result.valid) {
                    result.confidence = 1.0;
                    
                    // Well-known 포트 확인
                    if (port < 1024) {
                        result.metadata.put("category", "well_known");
                        result.metadata.put("service", getWellKnownService(port));
                    } else if (port < 49152) {
                        result.metadata.put("category", "registered");
                    } else {
                        result.metadata.put("category", "dynamic");
                    }
                }
            } catch (NumberFormatException e) {
                result.valid = false;
                result.confidence = 0.0;
            }
            
            return result;
        }
    }
}
```

#### 8.3.4 신뢰도 계산 상세

신뢰도는 0-100% 범위로 계산되며, 다음과 같은 상세한 공식을 사용합니다:

**1. 정규표현식 매칭 점수 (40%)**
```java
// Grok 패턴이 로그와 완전히 매칭되는지 확인
if (pattern.matches(logLine)) {
    // 매칭된 필드 수 계산
    int matchedFields = extractedFields.size();
    int totalFields = patternFields.size();

    regexScore = (matchedFields / totalFields) * 40;
}
```

**2. 필드 값 유효성 점수 (30%)**
```java
// 추출된 필드의 값이 예상 형식과 일치하는지 검증
- IP 필드: 유효한 IP 주소 형식인가?
- 포트 필드: 1-65535 범위의 숫자인가?
- 타임스탬프: 파싱 가능한 날짜 형식인가?
- HTTP 상태: 100-599 범위의 숫자인가?

validityScore = (validFields / totalFields) * 30;
```

**3. 구조적 유사도 점수 (20%)**
```java
// 로그의 구조적 특징 비교
- 구분자 패턴 일치도 (공백, 탭, 파이프, 콤마 등)
- 필드 개수 유사도
- 특수문자 위치 패턴
- 로그 길이 유사도

structureScore = calculateStructuralSimilarity() * 20;
```

**4. 키워드 매칭 점수 (10%)**
```java
// 포맷별 특징 키워드 존재 여부
예시:
- Apache: "GET", "POST", "HTTP/1.1", 상태코드
- Firewall: "ACCEPT", "DROP", "src=", "dst="
- Windows: "EventID", "Security", "Information"

keywordScore = (matchedKeywords / expectedKeywords) * 10;
```

##### 신뢰도 계산 공식

```
최종 신뢰도 = (기본 점수 × 가중치 합) + 그룹 보너스 - 패널티

기본 점수 구성:
- 정규표현식 매칭 (40%)
- 필드 유효성 (30%)
- 구조적 유사도 (20%)
- 키워드 매칭 (10%)

그룹 보너스: 0-20점
패널티: 0-30점
```

##### 가중치 및 임계값 설정

```java
public class ScoringConstants {
    // 기본 가중치
    public static final double REGEX_WEIGHT = 0.4;
    public static final double FIELD_WEIGHT = 0.3;
    public static final double STRUCTURE_WEIGHT = 0.2;
    public static final double KEYWORD_WEIGHT = 0.1;
    
    // 그룹별 최대 보너스
    public static final double FIREWALL_BONUS = 20.0;
    public static final double WEB_SERVER_BONUS = 25.0;
    public static final double SYSTEM_BONUS = 15.0;
    public static final double WAF_BONUS = 20.0;
    public static final double IPS_BONUS = 18.0;
    
    // 패널티 기준
    public static final double UNMATCHED_FIELD_PENALTY = 2.0;
    public static final double ERROR_PENALTY = 10.0;
    public static final double LOW_COVERAGE_PENALTY = 20.0;
    
    // 임계값
    public static final double MIN_CONFIDENCE = 50.0;
    public static final double HIGH_CONFIDENCE = 85.0;
    public static final double EXACT_MATCH = 95.0;
}
```

#### 8.3.5 그룹별 특화 알고리즘

각 로그 그룹의 특성에 맞춘 추가 평가 로직:

##### Firewall 그룹 특화 알고리즘
```java
public class FirewallSpecificScorer {
    
    public double calculateBonus(String log, MatchResult result) {
        double bonus = 0.0;
        
        // 필수 필드 체크 (각 2점)
        if (hasField(result, "src_ip") && hasField(result, "dst_ip")) {
            bonus += 4.0;
        }
        
        if (hasField(result, "src_port") && hasField(result, "dst_port")) {
            bonus += 4.0;
        }
        
        // 액션 키워드 (3점)
        if (log.matches(".*(ACCEPT|DROP|DENY|ALLOW|BLOCK|REJECT).*")) {
            bonus += 3.0;
        }
        
        // 프로토콜 정보 (2점)
        if (log.matches(".*(TCP|UDP|ICMP|GRE|ESP).*")) {
            bonus += 2.0;
        }
        
        // 방향성 표시 (2점)
        if (log.contains("->") || log.contains("<-") || 
            log.matches(".*(inbound|outbound|ingress|egress).*")) {
            bonus += 2.0;
        }
        
        // 인터페이스 정보 (2점)
        if (log.matches(".*(eth[0-9]|em[0-9]|ens[0-9]+|wan|lan).*")) {
            bonus += 2.0;
        }
        
        // 특정 벤더 패턴 (3점)
        bonus += detectVendorPattern(log);
        
        return Math.min(bonus, FIREWALL_BONUS);
    }
    
    private double detectVendorPattern(String log) {
        // Palo Alto
        if (log.contains("TRAFFIC") && log.contains("rule=")) {
            return 3.0;
        }
        
        // Fortinet
        if (log.contains("devname=") && log.contains("logid=")) {
            return 3.0;
        }
        
        // Cisco ASA
        if (log.matches(".*%ASA-\\d-\\d+:.*")) {
            return 3.0;
        }
        
        return 0.0;
    }
}
```

##### Web Server 그룹 특화 알고리즘
```java
public class WebServerSpecificScorer {
    
    public double calculateBonus(String log, MatchResult result) {
        double bonus = 0.0;
        
        // HTTP 메소드 (5점)
        if (log.matches(".*(GET|POST|PUT|DELETE|HEAD|OPTIONS|PATCH)\\s+.*")) {
            bonus += 5.0;
        }
        
        // URL 경로 (5점)
        if (log.matches(".*\\s+/[^\\s]*\\s+.*")) {
            bonus += 5.0;
        }
        
        // HTTP 버전 (3점)
        if (log.matches(".*HTTP/[0-9\\.]+.*")) {
            bonus += 3.0;
        }
        
        // 상태 코드 (4점)
        if (hasValidStatusCode(result)) {
            bonus += 4.0;
        }
        
        // 응답 크기/시간 (3점)
        if (hasField(result, "response_size") || hasField(result, "response_time")) {
            bonus += 3.0;
        }
        
        // 특정 포맷 식별 (5점)
        bonus += identifySpecificFormat(log);
        
        return Math.min(bonus, WEB_SERVER_BONUS);
    }
    
    private double identifySpecificFormat(String log) {
        // Apache 날짜 형식
        if (log.matches(".*\\[\\d{2}/\\w{3}/\\d{4}.*\\].*")) {
            return 5.0;
        }
        
        // Nginx 날짜 형식
        if (log.matches(".*\\d{4}/\\d{2}/\\d{2}\\s+\\d{2}:\\d{2}:\\d{2}.*")) {
            return 5.0;
        }
        
        // IIS 형식
        if (log.contains("cs-method") || log.contains("sc-status")) {
            return 5.0;
        }
        
        return 0.0;
    }
}
```

#### 8.3.6 최적화 전략

##### 성능 최적화
```java
public class OptimizationStrategy {
    
    // 1. 패턴 캐싱
    private final Map<String, CompiledPattern> patternCache = 
        new ConcurrentHashMap<>();
    
    // 2. 조기 종료 조건
    public boolean shouldContinue(double currentBestScore, double potentialScore) {
        // 현재 최고 점수를 넘을 가능성이 없으면 종료
        return potentialScore > currentBestScore - 10.0;
    }
    
    // 3. 병렬 처리
    public List<MatchResult> parallelMatch(String log, List<LogFormat> formats) {
        return formats.parallelStream()
            .map(format -> matchPattern(log, format))
            .filter(result -> result.matched)
            .collect(Collectors.toList());
    }
    
    // 4. 적응형 임계값
    public double getAdaptiveThreshold(int candidateCount) {
        if (candidateCount > 50) {
            return 80.0; // 후보가 많으면 높은 임계값
        } else if (candidateCount > 20) {
            return 70.0;
        } else {
            return 60.0; // 후보가 적으면 낮은 임계값
        }
    }
}
```

##### 메모리 최적화
```java
public class MemoryOptimization {
    
    // 1. 약한 참조를 사용한 캐시
    private final Map<String, WeakReference<CompiledPattern>> weakCache = 
        new ConcurrentHashMap<>();
    
    // 2. 결과 스트리밍
    public Stream<FormatRecommendation> streamRecommendations(
            String log, Stream<LogFormat> formats) {
        return formats
            .map(format -> matchPattern(log, format))
            .filter(result -> result.matched)
            .map(result -> createRecommendation(result))
            .sorted(Comparator.comparing(FormatRecommendation::getConfidence).reversed());
    }
    
    // 3. 필드 추출 지연 로딩
    public class LazyFieldExtraction {
        private final String log;
        private final Pattern pattern;
        private Map<String, String> fields;
        
        public Map<String, String> getFields() {
            if (fields == null) {
                fields = extractFields();
            }
            return fields;
        }
    }
}
```

#### 8.3.7 예외 케이스 처리

```java
public class EdgeCaseHandler {
    
    // 1. 다중 라인 로그 처리
    public MatchResult handleMultilineLog(List<String> lines, LogFormat format) {
        // Java 스택 트레이스
        if (format.getGroupName().equals("Application") && 
            lines.get(0).contains("Exception")) {
            return matchJavaStackTrace(lines, format);
        }
        
        // 일반 다중 라인
        String combined = String.join("\n", lines);
        return matchPattern(combined, format);
    }
    
    // 2. 인코딩 문제 처리
    public String normalizeEncoding(String log) {
        // UTF-8 BOM 제거
        if (log.startsWith("\uFEFF")) {
            log = log.substring(1);
        }
        
        // 잘못된 인코딩 문자 대체
        return log.replaceAll("[\\p{C}&&[^\t\n\r]]", "?");
    }
    
    // 3. 극단적 길이 처리
    public MatchResult handleExtremeLengthLog(String log, LogFormat format) {
        if (log.length() > MAX_LOG_LENGTH) {
            // 긴 로그는 청크로 분할하여 처리
            return matchInChunks(log, format);
        } else if (log.length() < MIN_LOG_LENGTH) {
            // 너무 짧은 로그는 부분 매칭 허용
            return partialMatch(log, format);
        }
        
        return matchPattern(log, format);
    }
    
    // 4. 특수 문자 이스케이프
    public String escapeSpecialCharacters(String log) {
        // 정규식 특수문자 이스케이프
        return log.replaceAll("([\\[\\]{}()\\\\^$.|?*+])", "\\\\$1");
    }
}
```

#### 8.3.3 신뢰도 계산 예시

**예시 1: Apache 로그**
```
입력: "192.168.1.1 - - [01/Jan/2024:10:00:00 +0900] \"GET /index.html HTTP/1.1\" 200 1234"

APACHE_HTTP_1.00 평가:
- 정규표현식 매칭: 40/40 (완전 매칭)
- 필드 유효성: 30/30 (모든 필드 유효)
- 구조적 유사도: 18/20 (전형적인 Apache 구조)
- 키워드 매칭: 10/10 (GET, HTTP, 200 등)
= 총 98%
```

**예시 2: 부분 매칭**
```
입력: "2024-01-01 10:00:00 ERROR Connection failed"

LINUX_SYSLOG_1.00 평가:
- 정규표현식 매칭: 20/40 (부분 매칭)
- 필드 유효성: 20/30 (타임스탬프, 레벨만 유효)
- 구조적 유사도: 10/20 (단순 구조)
- 키워드 매칭: 5/10 (ERROR 키워드만)
= 총 55%
```

#### 8.3.4 신뢰도 임계값

```java
public class ConfidenceLevel {
    public static final int EXACT_MATCH = 95;    // 거의 확실
    public static final int HIGH_CONFIDENCE = 85; // 높은 신뢰도
    public static final int MEDIUM_CONFIDENCE = 70; // 중간 신뢰도
    public static final int LOW_CONFIDENCE = 50;  // 낮은 신뢰도

    public String getConfidenceLabel(int score) {
        if (score >= EXACT_MATCH) return "확실";
        if (score >= HIGH_CONFIDENCE) return "높음";
        if (score >= MEDIUM_CONFIDENCE) return "중간";
        if (score >= LOW_CONFIDENCE) return "낮음";
        return "매우 낮음";
    }
}
```

#### 8.3.5 다중 패턴 처리

하나의 포맷이 여러 패턴을 가진 경우:
```java
// 각 패턴별로 신뢰도 계산 후 최고값 선택
for (Pattern pattern : format.getPatterns()) {
    int confidence = calculateConfidence(log, pattern);
    if (confidence > maxConfidence) {
        maxConfidence = confidence;
        bestPattern = pattern;
    }
}
```

#### 8.3.6 포맷 그룹별 특화 신뢰도 평가

각 로그 그룹의 특성에 맞춘 추가 평가 요소를 적용합니다:

**1. Firewall 그룹 (23개 포맷)**
```java
public class FirewallConfidenceEvaluator {
    // 필수 요소 체크 (추가 20% 가중치)
    - IP 주소 쌍 존재 (src_ip, dst_ip)
    - 포트 번호 쌍 존재 (src_port, dst_port)
    - 액션 키워드 (ACCEPT, DROP, DENY, ALLOW, BLOCK)
    - 프로토콜 필드 (TCP, UDP, ICMP)
    - 방향성 표시 (inbound, outbound, ->)

    // 특징적 패턴
    - 세션 ID 또는 규칙 ID 존재
    - 인터페이스 정보 (eth0, em1 등)
    - 패킷 크기/카운트 정보

    // 예시: PALOALTO_PANOS
    if (log.contains("TRAFFIC") && log.contains("rule=")) {
        confidence += 15; // 팔로알토 특유 키워드
    }
}
```

**2. Web Server 그룹 (4개 포맷)**
```java
public class WebServerConfidenceEvaluator {
    // HTTP 요청 라인 검증 (추가 25% 가중치)
    - HTTP 메소드 (GET, POST, PUT, DELETE, HEAD)
    - URL 경로 형식 (/로 시작)
    - HTTP 버전 (HTTP/1.0, HTTP/1.1, HTTP/2.0)
    - 상태 코드 (100-599)

    // 접근 로그 특징
    - User-Agent 문자열 패턴
    - Referer URL 형식
    - 응답 크기 (바이트)
    - 응답 시간 (밀리초)

    // 예시: APACHE vs NGINX 구분
    if (log.contains("\"") && log.matches(".*\\[\\d{2}/\\w{3}/\\d{4}.*")) {
        // Apache 날짜 형식: [25/Jul/2013:13:35:38]
        apacheScore += 10;
    } else if (log.matches(".*\\d{4}/\\d{2}/\\d{2}.*")) {
        // Nginx 날짜 형식: 2013/07/25
        nginxScore += 10;
    }
}
```

**3. System 그룹 (11개 포맷)**
```java
public class SystemConfidenceEvaluator {
    // 시스템 로그 특징 (추가 15% 가중치)
    - 프로세스 이름[PID] 패턴
    - Syslog Priority (<숫자>)
    - 시설(Facility) 및 심각도(Severity)
    - 시스템 이벤트 키워드

    // OS별 특징
    - Linux: sshd, systemd, kernel, cron
    - Windows: EventID, Security, System, Application
    - AIX/HP-UX: 특유의 날짜 형식 및 구조

    // 예시: SSH 로그 구분
    if (log.contains("sshd[") &&
        (log.contains("Accepted") || log.contains("Failed"))) {
        confidence += 20; // SSH 로그 확실
    }
}
```

**4. Web Firewall 그룹 (5개 포맷)**
```java
public class WAFConfidenceEvaluator {
    // WAF 특화 요소 (추가 20% 가중치)
    - 공격 유형 분류 (SQL Injection, XSS, Path Traversal)
    - 위험도 레벨 (High, Medium, Low, 높음, 중간, 낮음)
    - HTTP 요청 정보 (URL, Parameter)
    - 탐지/차단 액션

    // 공격 시그니처 패턴
    - SQL 키워드 (SELECT, UNION, DROP)
    - XSS 패턴 (<script>, javascript:)
    - 인코딩된 공격 패턴 (%27, %3C%3E)

    // 예시: MONITORAPP_AIWAF
    if (log.contains("DETECT|") && log.contains("|중간|")) {
        confidence += 15; // 모니터앱 특유 형식
    }
}
```

**5. IPS/IDS 그룹 (8개 포맷)**
```java
public class IPSConfidenceEvaluator {
    // IPS/IDS 특화 요소 (추가 18% 가중치)
    - 시그니처 ID 또는 규칙 ID
    - 공격명 또는 위협명
    - 우선순위/위험도 (Priority, Severity)
    - 공격자/피해자 IP 표시

    // 탐지 정보
    - CVE 번호 참조
    - 프로토콜 상세 (TCP 플래그 등)
    - 페이로드 또는 패킷 정보

    // 예시: SNORT 형식
    if (log.matches("\\[\\*\\*\\].*\\[\\*\\*\\]")) {
        confidence += 20; // Snort 특유의 [**] 구분자
    }
}
```

**6. APT 그룹 (2개 포맷)**
```java
public class APTConfidenceEvaluator {
    // APT 솔루션 특화 (추가 15% 가중치)
    - 악성코드 해시값 (MD5, SHA256)
    - 위협 인텔리전스 정보
    - 행위 분석 결과
    - 샌드박스 분석 점수

    // 예시: FireEye, Symantec APT
    if (log.contains("malware") && log.contains("hash")) {
        confidence += 15;
    }
}
```

**7. DDOS 그룹 (3개 포맷)**
```java
public class DDOSConfidenceEvaluator {
    // DDoS 특화 요소 (추가 17% 가중치)
    - 트래픽 볼륨 (pps, bps)
    - 공격 유형 (SYN Flood, UDP Flood, HTTP Flood)
    - 임계치 정보
    - 완화 액션 (Drop, Rate-limit)

    // 통계 정보
    - 세션 수
    - 패킷 통계
    - 대역폭 사용량
}
```

**8. Application 그룹 (20개 포맷)**
```java
public class ApplicationConfidenceEvaluator {
    // 애플리케이션별 특화 평가

    // 데이터베이스 (MySQL, PostgreSQL, MongoDB)
    if (format.getName().contains("SQL")) {
        - 쿼리 패턴 확인
        - 에러 코드 형식
        - 실행 시간 정보
    }

    // 메시지 큐 (RabbitMQ, Kafka)
    if (format.getName().contains("MQ")) {
        - 큐/토픽 이름
        - 메시지 ID
        - 파티션 정보
    }

    // 웹 애플리케이션 서버 (Tomcat, JBoss)
    if (format.getName().contains("TOMCAT")) {
        - 스택 트레이스 패턴
        - 스레드 이름
        - 로그 레벨 (INFO, WARN, ERROR)
    }
}
```

#### 8.3.4 전체 매칭 플로우

```java
public class SimplifiedFormatMatcher {
    
    public List<FormatRecommendation> recommendFormats(String log) {
        // 1. 로그 정규화
        String normalizedLog = normalizeLog(log);
        
        // 2. 모든 포맷에 대해 Grok 매칭 시도 (병렬 처리)
        List<MatchResult> allMatches = getAllFormats().parallelStream()
            .flatMap(format -> matchFormat(normalizedLog, format).stream())
            .collect(Collectors.toList());
        
        // 3. 완전 매칭과 부분 매칭 분리
        Map<Boolean, List<MatchResult>> partitioned = allMatches.stream()
            .collect(Collectors.partitioningBy(MatchResult::isCompleteMatch));
        
        List<MatchResult> perfectMatches = partitioned.get(true);
        List<MatchResult> partialMatches = partitioned.get(false);
        
        // 4. 결과 처리
        if (!perfectMatches.isEmpty()) {
            // 완전 매칭이 있으면 그것만 사용
            if (perfectMatches.size() == 1) {
                // 단일 완전 매칭 - 98% 신뢰도
                return Arrays.asList(
                    createRecommendation(perfectMatches.get(0), 98.0)
                );
            } else {
                // 다중 완전 매칭 - 추가 평가
                return evaluateMultipleMatches(perfectMatches);
            }
        } else if (!partialMatches.isEmpty()) {
            // 완전 매칭 없음 - 부분 매칭 평가
            return evaluatePartialMatches(partialMatches);
        } else {
            // 매칭 없음
            return Collections.emptyList();
        }
    }
    
    // 포맷별 매칭 시도
    private List<MatchResult> matchFormat(String log, LogFormat format) {
        List<MatchResult> results = new ArrayList<>();
        
        for (Pattern pattern : format.getPatterns()) {
            try {
                MatchResult result = tryGrokMatch(log, format, pattern);
                if (result != null) {
                    results.add(result);
                }
            } catch (Exception e) {
                // 패턴 오류는 무시하고 계속
            }
        }
        
        return results;
    }
}
```

#### 8.3.5 실제 계산 예시 (단순화된 버전)

##### 예시 1: 단일 완전 매칭

```
입력 로그:
"2024-01-15 10:30:45 FW01 TRAFFIC ACCEPT src=192.168.1.100 dst=8.8.8.8 
sport=54321 dport=443 proto=TCP rule=allow-https"

매칭 과정:

1. Grok 매칭 시도
   - 전체 포맷에 대해 병렬 매칭
   - FORTINET_FG_1.00: 완전 매칭 ✓
   - 다른 포맷들: 매칭 실패 또는 부분 매칭

2. 결과 분석
   - 완전 매칭: 1개 (FORTINET_FG_1.00)
   - 부분 매칭: 여러 개 (무시됨)

3. 즉시 반환
   포맷: FORTINET_FG_1.00
   신뢰도: 98% (단일 완전 매칭)
   
   추출된 필드:
   - timestamp: "2024-01-15 10:30:45"
   - device_name: "FW01"
   - action: "ACCEPT"
   - src_ip: "192.168.1.100"
   - dst_ip: "8.8.8.8"
   - src_port: "54321"
   - dst_port: "443"
   - protocol: "TCP"
   - rule_name: "allow-https"
```

##### 예시 2: 다중 완전 매칭

```
입력 로그:
"2024-01-15 10:30:45 ERROR Failed to connect"

매칭 과정:

1. Grok 매칭 시도
   - GENERAL_LOG: "%{TIMESTAMP} %{LOGLEVEL} %{GREEDYDATA}" ✓
   - SIMPLE_ERROR: "%{ISO8601} ERROR %{MESSAGE}" ✓
   - APP_LOG: "%{DATE} %{TIME} %{WORD:level} %{GREEDYDATA:msg}" ✓

2. 다중 매칭 평가
   
   SIMPLE_ERROR (97%):
   - 기본: 95%
   - 필드 수: 3개 (+1%)
   - GREEDYDATA: 1개 (-2%)
   - 패턴 구체성: +3%
   
   APP_LOG (96%):
   - 기본: 95%
   - 필드 수: 4개 (+1%)
   - GREEDYDATA: 1개 (-2%)
   - 패턴 구체성: +2%
   
   GENERAL_LOG (91%):
   - 기본: 95%
   - 필드 수: 2개 (+0%)
   - GREEDYDATA: 1개 (-2%)
   - 패턴 구체성: -2%

3. 최종 결과
   1. SIMPLE_ERROR (97%)
   2. APP_LOG (96%)
   3. GENERAL_LOG (91%)
```

##### 예시 3: 부분 매칭만 있는 경우

```
입력 로그:
"Connection timeout at 192.168.1.100"

매칭 과정:

1. Grok 매칭 시도
   - 완전 매칭: 없음
   - 부분 매칭: 여러 개

2. 부분 매칭 평가
   
   NETWORK_ERROR (45%):
   - 필드 수: 1개 (IP만) → 5점
   - 중요 필드: IP 있음 → 5점
   - 커버리지: 30% → 3점
   - 총점: 13/70 × 100 = 45%
   
   GENERIC_LOG (35%):
   - 필드 수: 1개 → 5점
   - 중요 필드: 없음 → 0점
   - 커버리지: 20% → 2점
   - 총점: 7/70 × 100 = 35%

3. 최종 결과
   1. NETWORK_ERROR (45%)
   2. GENERIC_LOG (35%)
   (30% 미만은 제외)
```

#### 8.3.10 알고리즘 복잡도 분석

```
시간 복잡도:
- 사전 필터링: O(n) where n = 전체 포맷 수
- 패턴 매칭: O(m × p) where m = 후보 수, p = 패턴 복잡도
- 신뢰도 계산: O(m × f) where f = 필드 수
- 최종 정렬: O(m log m)

전체: O(n + m×p + m×f + m log m)
일반적으로 m << n 이므로 효율적

공간 복잡도:
- 패턴 캐시: O(n)
- 매칭 결과: O(m × f)
- 전체: O(n + m×f)
```

#### 8.3.6 알고리즘 장점 및 특징

**단순화된 알고리즘의 장점:**

1. **명확한 우선순위**
   - Grok 완전 매칭이 최우선
   - 단일 매칭 시 즉시 반환으로 빠른 응답
   - 복잡한 계산은 필요할 때만 수행

2. **높은 정확도**
   - 완전 매칭 시 95%+ 신뢰도 보장
   - GREEDYDATA 패널티로 더 정확한 패턴 우선
   - 그룹별 특화 평가는 선택적 적용

3. **성능 최적화**
   - 병렬 처리로 빠른 매칭
   - 조기 종료 조건으로 불필요한 계산 방지
   - 메모리 효율적인 스트림 처리

4. **유지보수 용이**
   - 단순한 로직으로 디버깅 쉬움
   - 새로운 포맷 추가 시 별도 조정 불필요
   - 가중치 조정이 최소화됨

#### 8.3.7 매칭 알고리즘 요약

**핵심 원칙: "Grok 완전 매칭이 답이다"**

```
신뢰도 계산 규칙:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
상황                     신뢰도    처리
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
단일 완전 매칭           98%      즉시 반환
다중 완전 매칭           95-97%   추가 평가
부분 매칭만 존재         30-70%   제한적 평가
매칭 없음               0%       결과 없음
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

추가 평가 기준 (다중 완전 매칭 시):
• 필드 추출 개수 (많을수록 우선)
• GREEDYDATA 사용 최소화
• 패턴 길이 (구체적일수록 우선)
• 그룹별 특화 키워드 존재 여부
```

이 단순화된 접근법은 대부분의 경우에서 빠르고 정확한 결과를 제공하며, 
복잡한 가중치 계산을 최소화하여 유지보수가 용이합니다.


### 8.4 데이터 관리

#### 8.4.1 패턴 데이터 로딩
- GROK-PATTERN-CONVERTER.sql 파일 파싱
- 메모리 내 인덱싱 및 캐싱
- 런타임 패턴 업데이트 지원

#### 8.4.2 오프라인 지원
- 첫 실행 시 API에서 데이터 가져와 캐시
- 네트워크 오류 시 캐시된 데이터 사용
- --offline 옵션으로 강제 오프라인 모드



### 8.5 설정 파일 (application.properties)
```properties
# 매칭 설정
matcher.min.confidence=70
matcher.max.results=5
matcher.timeout.ms=5000

# 로깅
logging.level=INFO
logging.file=${user.home}/.format-recommend/logs/format-recommend.log
```

## 9. 사용자 인터페이스

### 9.1 웹 인터페이스
- **로그 입력 화면**
  - 텍스트 에디터 with 구문 강조
  - 드래그 앤 드롭 파일 업로드
  - 샘플 로그 예제 제공

- **추천 결과 화면**
  - 추천 포맷 카드 형태 표시
  - 매칭률 시각적 표시 (프로그레스 바)
  - 필드 매핑 테이블
  - 적용 전 미리보기

### 9.2 CLI 인터페이스
```bash
# 단일 로그 추천
$ lfrs recommend "2024-01-15 10:30:45 ERROR Connection timeout"

# 파일 기반 추천
$ lfrs recommend -f /path/to/logfile.log

# 상세 옵션
$ lfrs recommend -f logfile.log --max-results 5 --min-confidence 0.8 --output json
```

## 10. 테스트 계획

### 10.1 단위 테스트
- Grok 패턴 파싱 정확도
- 필드 추출 검증
- 매칭 알고리즘 정확도
- 성능 벤치마크

### 10.2 통합 테스트
- API 엔드포인트 테스트
- 파일 업로드 처리
- 대용량 로그 처리
- 동시성 테스트

### 10.3 사용자 수용 테스트
- 실제 로그 샘플 테스트
- 추천 정확도 검증
- 사용성 평가

## 11. 배포 및 운영

### 11.1 배포 전략
- **초기 배포**: 파일럿 사용자 그룹
- **단계적 확대**: 피드백 기반 개선 후 전체 배포
- **버전 관리**: Semantic Versioning (MAJOR.MINOR.PATCH)

### 11.2 모니터링
- **성능 메트릭**
  - API 응답 시간
  - 추천 정확도
  - 시스템 리소스 사용률
- **비즈니스 메트릭**
  - 일일 활성 사용자
  - 평균 추천 사용 횟수
  - 포맷 선택률

### 11.3 유지보수
- **패턴 업데이트**: 월 1회 정기 업데이트
- **성능 최적화**: 분기별 성능 리뷰
- **버그 수정**: 심각도에 따른 대응 (Critical: 24시간, Major: 1주일)

## 12. 프로젝트 일정

### Phase 1: 기반 구축 (4주)
- Week 1-2: 아키텍처 설계 및 개발 환경 구축
- Week 3-4: Grok 엔진 통합 및 기본 매칭 구현

### Phase 2: 핵심 기능 개발 (6주)
- Week 5-6: 로그 파서 및 패턴 매처 구현
- Week 7-8: 추천 알고리즘 개발
- Week 9-10: API 개발 및 SDK 구현

### Phase 3: 고도화 및 최적화 (4주)
- Week 11-12: 성능 최적화 및 확장성 개선
- Week 13-14: UI 개발 및 통합 테스트

### Phase 4: 배포 준비 (2주)
- Week 15: 사용자 수용 테스트
- Week 16: 문서화 및 배포 준비

## 13. 리스크 및 대응 방안

### 13.1 기술적 리스크
- **리스크**: Grok 패턴 매칭 성능 저하
  - **대응**: 패턴 캐싱 및 인덱싱 구현

- **리스크**: JDK 1.8 호환성 이슈
  - **대응**: 호환성 테스트 강화, 대체 라이브러리 조사

### 13.2 비즈니스 리스크
- **리스크**: 낮은 추천 정확도
  - **대응**: 머신러닝 기반 개선 방안 검토

- **리스크**: 사용자 채택률 저조
  - **대응**: 사용자 교육 및 가이드 제공

## 14. 성공 지표 (KPI)

- **기술적 KPI**
  - 추천 정확도: 95% 이상
  - 평균 응답 시간: 500ms 이하
  - 시스템 가용성: 99.9%

- **비즈니스 KPI**
  - 로그 설정 시간: 90% 단축
  - 사용자 만족도: 4.5/5.0 이상
  - 월간 활성 사용자: 1,000명 이상

## 15. 부록

### 15.1 용어 정의
- **Grok**: Logstash에서 사용하는 정규식 기반 패턴 매칭 도구
- **SIEM**: Security Information and Event Management
- **로그 포맷**: 로그 데이터의 구조와 필드를 정의한 템플릿

### 15.2 참고 자료
- Grok Patterns Reference: https://github.com/logstash-plugins/logstash-patterns-core
- Java Grok Library: https://github.com/thekrakken/java-grok
- SIEM Best Practices Guide

### 15.3 변경 이력
- v1.0 (2024-08-05): 초기 버전 작성

## 16. 로그 포맷별 주요 필드

### 16.1 Firewall 그룹 주요 필드
공통적으로 추출되는 필드:
- **src_ip/dst_ip**: 출발지/목적지 IP 주소
- **src_port/dst_port**: 출발지/목적지 포트
- **protocol**: 프로토콜 (TCP/UDP/ICMP)
- **action**: 허용/차단 액션
- **rule_name/rule_id**: 적용된 정책
- **interface**: 네트워크 인터페이스
- **sent_size/recv_size**: 송수신 데이터 크기

### 16.2 Web Server 그룹 주요 필드
- **src_ip**: 클라이언트 IP
- **method**: HTTP 메소드 (GET/POST 등)
- **url**: 요청 URL
- **status**: HTTP 상태 코드
- **sent_size**: 응답 크기
- **user_agent**: 브라우저 정보
- **response_time**: 응답 시간

### 16.3 System 그룹 주요 필드
- **event_name**: 이벤트 이름 (sshd, useradd 등)
- **event_id**: 프로세스 ID
- **action**: 수행된 작업
- **user_id**: 사용자 ID
- **message**: 상세 메시지

### 16.4 추천 정확도 향상 요소

**1. 필드 특화 패턴**
- IP 주소 패턴: 정확한 IPv4 형식 검증
- 포트 패턴: 1-65535 범위 검증
- 날짜/시간 패턴: 10가지 다양한 형식 지원

**2. 벤더별 특화 키워드**
- Palo Alto: "TRAFFIC", "rule="
- Fortinet: "devname=", "logid="
- Cisco ASA: "%ASA-"
- Check Point: "src:", "dst:"

**3. 그룹별 필수 요소**
- Firewall: IP 쌍, 포트 쌍, 액션
- Web Server: HTTP 메소드, URL, 상태 코드
- System: 이벤트명, 사용자 정보
- IPS: 공격 유형, 위험도

## 17. 설치 및 배포

### 17.1 설치 방법

#### 방법 1: JAR 직접 실행
```bash
# 다운로드
$ wget https://github.com/project/format-recommend/releases/latest/format-recommend.jar

# 실행
$ java -jar format-recommend.jar "로그 샘플"
```

#### 방법 2: 시스템 설치
```bash
# 스크립트 방식
$ curl -sSL https://install.format-recommend.io | bash

# 또는 수동 설치
$ sudo cp format-recommend.jar /usr/local/lib/
$ sudo cp format-recommend /usr/local/bin/
$ sudo chmod +x /usr/local/bin/format-recommend
```

### 16.2 실행 스크립트 (format-recommend)
```bash
#!/bin/bash
java -jar /usr/local/lib/format-recommend.jar "$@"
```

## 17. 사용 예제

### 17.1 기본 사용
```bash
# 첫 실행 (API에서 포맷 정보 자동 다운로드)
$ format-recommend "128.134.225.3 - - [25/Jul/2013:13:35:38 +0900] \"GET /index.html HTTP/1.1\" 200 3323"
[INFO] 포맷 정보를 다운로드하는 중... (최초 1회)
[INFO] 100개 포맷 정보 로드 완료

[1] APACHE_HTTP_1.00 (98% - 확실)
    그룹: Web Server
    패턴: APACHE_HTTP_1.00_1
    매칭 상세:
    - 정규표현식: 40/40
    - 필드 유효성: 30/30
    - 구조 유사도: 18/20
    - 키워드: 10/10

[2] NGINX_ACCESS_1.00 (72% - 중간)
    그룹: Web Server
    패턴: NGINX_ACCESS_1.00_1

# 신뢰도 임계값 설정
$ format-recommend -c 90 "로그 샘플"  # 90% 이상만 표시

# 상세 신뢰도 정보 확인
$ format-recommend -v "로그 샘플"
```

### 17.2 캐시 관리
```bash
# 캐시 수동 업데이트
$ format-recommend --update-cache
[INFO] API에서 최신 포맷 정보를 가져오는 중...
[INFO] 100개 포맷 정보 업데이트 완료

# 오프라인 모드 (캐시만 사용)
$ format-recommend --offline "로그 샘플"
[INFO] 오프라인 모드: 캐시된 데이터 사용 (2024-01-15 10:30:00)
```

### 17.3 API URL 변경
```bash
# 임시로 다른 API 사용
$ format-recommend --api-url "http://192.168.1.100:8080/api/formats" "로그 샘플"

# 설정 파일에서 영구 변경
$ echo "api.url=http://새로운주소/api" >> ~/.format-recommend/config.properties
```

### 17.4 파일 처리
```bash
# 단일 파일
$ format-recommend -f /var/log/apache2/access.log

# 여러 파일
$ format-recommend -f log1.txt log2.txt log3.txt

# 디렉토리
$ format-recommend -d /var/log/ --json > analysis.json
```

### 17.5 파이프라인 통합
```bash
# tail과 함께 사용
$ tail -f /var/log/syslog | format-recommend -

# 다른 도구와 연계
$ cat unknown.log | format-recommend -p | jq '.fields'
```

### 17.6 스크립트에서 사용
```bash
#!/bin/bash
FORMAT=$(format-recommend -q "$1" | head -1)
if [[ $FORMAT == "APACHE_HTTP"* ]]; then
    echo "Apache 로그 감지"
fi
```

### 17.7 그룹별 필터링
```bash
# Firewall 그룹만 검색
$ format-recommend -g "Firewall" "로그 샘플"

# 사용 가능한 그룹 확인
$ format-recommend --list | grep "그룹:" | sort | uniq
그룹: Application
그룹: APT
그룹: Beats
그룹: DDOS
그룹: Firewall
그룹: IPS
그룹: System
그룹: Web Firewall
그룹: Web Server
```

### 17.8 그룹별 신뢰도 평가 예시

**Firewall 로그 예시**
```bash
$ format-recommend "2024-01-15 10:30:45 FW-ACCEPT src=192.168.1.10 dst=8.8.8.8 proto=TCP sport=54321 dport=443"

[1] GENERIC_FIREWALL_1.00 (92% - 확실)
    기본 매칭: 72%
    + Firewall 그룹 보너스: 20%
      - IP 쌍 확인 ✓
      - 포트 쌍 확인 ✓
      - 액션 키워드 (ACCEPT) ✓
      - 프로토콜 (TCP) ✓
```

**Web Server 로그 예시**
```bash
$ format-recommend -v '10.0.0.1 - - [15/Jan/2024:10:30:45 +0900] "GET /api/users HTTP/1.1" 200 1234'

[1] APACHE_HTTP_1.00 (98% - 확실)
    기본 매칭: 73%
    + Web Server 그룹 보너스: 25%
      - HTTP 메소드 (GET) ✓
      - URL 경로 형식 ✓
      - HTTP 버전 ✓
      - 상태 코드 (200) ✓
      - Apache 날짜 형식 ✓
```

**System 로그 예시**
```bash
$ format-recommend "Jan 15 10:30:45 server01 sshd[12345]: Failed password for root from 192.168.1.100"

[1] LINUX_SYSLOG_1.00 (88% - 높음)
    기본 매칭: 68%
    + System 그룹 보너스: 20%
      - 프로세스[PID] 패턴 ✓
      - SSH 키워드 감지 ✓
      - 시스템 이벤트 패턴 ✓
```

문서의 나머지 섹션은 아래와 같이 이어집니다.
```bash
$ format-recommend "잘못된 로그"
에러: 매칭되는 포맷을 찾을 수 없습니다.
힌트: --verbose 옵션으로 상세 정보를 확인하세요.
```

### 10.2 네트워크 에러
```bash
$ format-recommend "로그 샘플"
[WARN] API 연결 실패: Connection timeout
[INFO] 캐시된 데이터 사용 (2024-01-14 15:30:00)
```

### 10.3 디버그 모드
```bash
$ format-recommend --verbose "로그 샘플"
[DEBUG] API URL: http://192.168.1.75:9999/lcnet/groklogformat/getAllFormat6Patterns
[DEBUG] 캐시 확인: ~/.format-recommend/cache/formats.json
[DEBUG] 캐시 유효성: OK (남은 시간: 18시간)
[DEBUG] 로드된 포맷 수: 100개
[DEBUG] 타임스탬프 패턴 감지: ISO8601
[DEBUG] 구분자: 공백
[DEBUG] 후보 포맷: 15개
[DEBUG] 매칭 시도: APACHE_HTTP_1.00...
[DEBUG]   - 정규표현식 매칭: 성공 (40/40)
[DEBUG]   - 필드 유효성: 30/30
[DEBUG]   - 구조 유사도: 18/20
[DEBUG]   - 키워드 매칭: 10/10
[DEBUG]   - Web Server 그룹 보너스: +25
[DEBUG]   - 최종 신뢰도: 98%
```

### 10.4 포맷별 특수 케이스 처리

```bash
# 다중 라인 로그 (Java Stack Trace)
$ format-recommend -f error.log
[INFO] 다중 라인 패턴 감지: Java Exception
[INFO] 3개 라인을 하나의 로그 엔트리로 처리

# 한글 포함 로그
$ format-recommend "2024-01-15 10:30:45 경고: 연결 실패"
[INFO] 한글 로그 감지: System 그룹 우선 검색

# 특수 구분자 로그
$ format-recommend "data1|data2|data3|data4"
[INFO] 파이프(|) 구분자 감지: CSV 형식 포맷 우선 검색
```

## 18. 추천 시스템 예상 시나리오

### 18.1 Firewall 로그 추천 예시
```
입력 로그: "2024-01-15 10:30:45 FW01 TRAFFIC ACCEPT src=192.168.1.100 dst=8.8.8.8 sport=54321 dport=443 proto=TCP"

추천 결과:
1. FORTINET_FG600C (98% - Grok 완전 매칭)
2. PALOALTONETWORKS_PALOALTO (95% - 유사 패턴)
3. SECUI_MF2 (92% - 유사 패턴)
```

### 18.2 Web Server 로그 추천 예시
```
입력 로그: "192.168.1.1 - - [15/Jan/2024:10:30:45 +0900] \"GET /index.html HTTP/1.1\" 200 1234"

추천 결과:
1. APACHE_HTTP (98% - Grok 완전 매칭)
2. TMAXSOFT_WEBTOB (85% - 유사 날짜 형식)
```

### 18.3 추천 알고리즘 핵심
- **단일 Grok 완전 매칭**: 즉시 98% 신뢰도로 반환
- **다중 완전 매칭**: 필드 수, GREEDYDATA 사용 여부로 우선순위 결정
- **부분 매칭**: 최대 70% 신뢰도로 제한

위 문서는 SIEM 로그포맷 추천 시스템의 상세한 제품 요구사항을 담고 있습니다. 
GROK-PATTERN-CONVERTER.sql에 정의된 100개의 로그 포맷을 대상으로 최적의 포맷을 자동으로 추천합니다.
