# LogCenter Format Recommender API 문서

## 목차
1. [개요](#개요)
2. [인증](#인증)
3. [엔드포인트](#엔드포인트)
4. [데이터 모델](#데이터-모델)
5. [오류 처리](#오류-처리)
6. [예제](#예제)
7. [SDK 사용법](#sdk-사용법)

## 개요

LogCenter Format Recommender API는 RESTful 웹 서비스로, 로그 포맷 추천 기능을 원격으로 제공합니다.

### 기본 정보
- **Base URL**: `http://api.example.com`
- **버전**: v1
- **프로토콜**: HTTPS
- **인코딩**: UTF-8
- **Content-Type**: application/json

### 제한 사항
- **요청 크기**: 최대 10MB
- **Rate Limiting**: 분당 100회
- **타임아웃**: 30초

## 인증

### Bearer Token
모든 API 요청에는 Bearer 토큰이 필요합니다.

```bash
curl -H "Authorization: Bearer YOUR_API_KEY" \
     https://api.example.com/api/v1/logformats
```

### API 키 발급
1. 관리자 콘솔 접속
2. API Keys 메뉴에서 새 키 생성
3. 권한 설정 (읽기/쓰기)
4. 키 안전하게 보관

## 엔드포인트

### 1. Health Check
서버 상태 확인

**요청**
```http
GET /api/v1/health
```

**응답**
```json
{
  "status": "healthy",
  "version": "1.0.0",
  "timestamp": 1722844800000
}
```

### 2. 로그 포맷 목록 조회
사용 가능한 모든 로그 포맷 조회

**요청**
```http
GET /api/v1/logformats
```

**쿼리 파라미터**
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| group | string | 아니오 | 그룹별 필터링 |
| vendor | string | 아니오 | 벤더별 필터링 |
| page | integer | 아니오 | 페이지 번호 (기본: 1) |
| size | integer | 아니오 | 페이지 크기 (기본: 50) |

**응답**
```json
{
  "success": true,
  "message": "로그 포맷 목록 조회 성공",
  "data": [
    {
      "formatId": "APACHE_HTTP_1.00",
      "formatName": "APACHE_HTTP",
      "formatVersion": "1.00",
      "groupName": "WEBSERVER",
      "vendor": "APACHE",
      "model": "HTTP",
      "logTypes": [
        {
          "typeName": "Access Log",
          "patterns": [
            {
              "expName": "APACHE_HTTP_1.00_1",
              "grokExp": "^%{IP:client_ip} - - \\[%{HTTPDATE:timestamp}\\]...",
              "sampleLog": "192.168.1.100 - - [05/Aug/2025:10:15:30 +0900]..."
            }
          ]
        }
      ]
    }
  ],
  "pagination": {
    "page": 1,
    "size": 50,
    "totalElements": 102,
    "totalPages": 3
  },
  "timestamp": 1722844800000
}
```

### 3. 특정 로그 포맷 조회
포맷 ID로 상세 정보 조회

**요청**
```http
GET /api/v1/logformats/{formatId}
```

**경로 파라미터**
- `formatId`: 로그 포맷 ID (예: APACHE_HTTP_1.00)

**응답**
```json
{
  "success": true,
  "message": "로그 포맷 조회 성공",
  "data": {
    "formatId": "APACHE_HTTP_1.00",
    "formatName": "APACHE_HTTP",
    "formatVersion": "1.00",
    "groupName": "WEBSERVER",
    "vendor": "APACHE",
    "model": "HTTP",
    "logTypes": [...],
    "metadata": {
      "created": "2025-01-01T00:00:00Z",
      "updated": "2025-08-01T00:00:00Z",
      "usage": 15234
    }
  },
  "timestamp": 1722844800000
}
```

### 4. 로그 포맷 추천
로그 샘플을 분석하여 적합한 포맷 추천

**요청**
```http
POST /api/v1/recommend
```

**요청 본문**
```json
{
  "log_samples": [
    "192.168.1.100 - - [05/Aug/2025:10:15:30 +0900] \"GET /index.html HTTP/1.1\" 200 1234"
  ],
  "group_filter": "WEBSERVER",
  "vendor_filter": null,
  "top_n": 5,
  "include_metadata": true,
  "parallel_processing": true
}
```

**요청 파라미터**
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| log_samples | array | 예 | 분석할 로그 샘플 (최대 100개) |
| group_filter | string | 아니오 | 특정 그룹으로 제한 |
| vendor_filter | string | 아니오 | 특정 벤더로 제한 |
| top_n | integer | 아니오 | 상위 N개 결과 (기본: 10) |
| include_metadata | boolean | 아니오 | 메타데이터 포함 여부 |
| parallel_processing | boolean | 아니오 | 병렬 처리 사용 (기본: true) |

**응답**
```json
{
  "success": true,
  "message": "로그 포맷 추천 완료",
  "data": [
    {
      "formatId": "APACHE_HTTP_1.00",
      "formatName": "APACHE_HTTP",
      "confidence": 98.5,
      "matchedFields": {
        "client_ip": "192.168.1.100",
        "timestamp": "05/Aug/2025:10:15:30 +0900",
        "method": "GET",
        "path": "/index.html",
        "protocol": "HTTP/1.1",
        "status": "200",
        "bytes": "1234"
      },
      "metadata": {
        "matchTime": 15,
        "patternUsed": "APACHE_HTTP_1.00_1",
        "fieldCount": 7
      }
    }
  ],
  "statistics": {
    "totalSamples": 1,
    "successfulMatches": 1,
    "failedMatches": 0,
    "processingTime": 25
  },
  "timestamp": 1722844800000
}
```

### 5. 배치 추천
여러 로그 샘플에 대한 일괄 추천

**요청**
```http
POST /api/v1/recommend/batch
```

**요청 본문**
```json
{
  "batch_requests": [
    {
      "id": "req-001",
      "log_sample": "192.168.1.100 - - [05/Aug/2025:10:15:30 +0900]..."
    },
    {
      "id": "req-002",
      "log_sample": "Aug  5 10:15:30 server01 sshd[1234]: Accepted..."
    }
  ],
  "common_options": {
    "top_n": 3,
    "group_filter": null
  }
}
```

**응답**
```json
{
  "success": true,
  "message": "배치 추천 완료",
  "data": {
    "req-001": {
      "recommendations": [...],
      "status": "success"
    },
    "req-002": {
      "recommendations": [...],
      "status": "success"
    }
  },
  "summary": {
    "total": 2,
    "successful": 2,
    "failed": 0
  },
  "timestamp": 1722844800000
}
```

### 6. 통계 조회
시스템 사용 통계 조회

**요청**
```http
GET /api/v1/statistics
```

**쿼리 파라미터**
- `start_date`: 시작 날짜 (ISO 8601)
- `end_date`: 종료 날짜 (ISO 8601)
- `group_by`: 그룹 기준 (day|week|month)

**응답**
```json
{
  "success": true,
  "message": "통계 조회 성공",
  "data": {
    "totalRequests": 15234,
    "successRate": 95.8,
    "topFormats": [
      {
        "formatName": "APACHE_HTTP",
        "count": 4521,
        "percentage": 29.7
      }
    ],
    "dailyStats": [...]
  },
  "timestamp": 1722844800000
}
```

## 데이터 모델

### ApiResponse
모든 API 응답의 기본 구조

```typescript
interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  errors?: string[];
  timestamp: number;
}
```

### LogFormat
로그 포맷 정보

```typescript
interface LogFormat {
  formatId: string;
  formatName: string;
  formatVersion: string;
  groupName: string;
  vendor: string;
  model: string;
  logTypes: LogType[];
}

interface LogType {
  typeName: string;
  typeDescription: string;
  patterns: Pattern[];
}

interface Pattern {
  expName: string;
  grokExp: string;
  sampleLog: string;
  order: string;
}
```

### FormatRecommendation
추천 결과

```typescript
interface FormatRecommendation {
  formatId: string;
  formatName: string;
  confidence: number;
  matchedFields: Record<string, string>;
  metadata?: {
    matchTime: number;
    patternUsed: string;
    fieldCount: number;
  };
}
```

## 오류 처리

### HTTP 상태 코드
| 코드 | 설명 |
|------|------|
| 200 | 성공 |
| 400 | 잘못된 요청 |
| 401 | 인증 실패 |
| 403 | 권한 없음 |
| 404 | 리소스 없음 |
| 429 | 요청 제한 초과 |
| 500 | 서버 오류 |

### 오류 응답 형식
```json
{
  "success": false,
  "message": "오류 설명",
  "errors": [
    "상세 오류 메시지 1",
    "상세 오류 메시지 2"
  ],
  "timestamp": 1722844800000
}
```

## 예제

### cURL 예제
```bash
# 로그 포맷 추천
curl -X POST https://api.example.com/api/v1/recommend \
  -H "Authorization: Bearer YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "log_samples": ["192.168.1.100 - - [05/Aug/2025:10:15:30 +0900] \"GET /index.html HTTP/1.1\" 200 1234"],
    "top_n": 3
  }'
```

### Python 예제
```python
import requests

# API 클라이언트 설정
api_url = "https://api.example.com/api/v1"
headers = {
    "Authorization": "Bearer YOUR_API_KEY",
    "Content-Type": "application/json"
}

# 로그 포맷 추천
def recommend_format(log_sample):
    response = requests.post(
        f"{api_url}/recommend",
        headers=headers,
        json={
            "log_samples": [log_sample],
            "top_n": 5
        }
    )
    return response.json()

# 사용 예제
result = recommend_format("192.168.1.100 - - [05/Aug/2025:10:15:30 +0900]...")
print(f"추천 포맷: {result['data'][0]['formatName']}")
print(f"신뢰도: {result['data'][0]['confidence']}%")
```

### JavaScript 예제
```javascript
// API 클라이언트
class LogFormatClient {
  constructor(apiKey) {
    this.apiKey = apiKey;
    this.baseUrl = 'https://api.example.com/api/v1';
  }

  async recommendFormat(logSample) {
    const response = await fetch(`${this.baseUrl}/recommend`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${this.apiKey}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        log_samples: [logSample],
        top_n: 5
      })
    });
    
    return response.json();
  }
}

// 사용 예제
const client = new LogFormatClient('YOUR_API_KEY');
const result = await client.recommendFormat('192.168.1.100 - - ...');
console.log(`추천 포맷: ${result.data[0].formatName}`);
```

## SDK 사용법

### Java SDK
```java
// Maven dependency
<dependency>
    <groupId>com.logcenter</groupId>
    <artifactId>logformat-client</artifactId>
    <version>1.0.0</version>
</dependency>

// 사용 예제
LogFormatApiClient client = new LogFormatApiClient(
    "https://api.example.com",
    "YOUR_API_KEY"
);

// 로그 포맷 추천
LogFormatRequest request = new LogFormatRequest();
request.setLogSamples(Arrays.asList("192.168.1.100 - - ..."));
request.setTopN(5);

List<FormatRecommendation> recommendations = client.recommendFormats(request);
for (FormatRecommendation rec : recommendations) {
    System.out.printf("%s: %.1f%%\n", rec.getFormatName(), rec.getConfidence());
}

// 캐시 사용
client.clearCache(); // 캐시 초기화
```

### 웹훅 통합
```json
{
  "webhook_url": "https://your-webhook.com/endpoint",
  "events": ["recommendation.created", "format.updated"],
  "secret": "webhook-secret"
}
```

---
© 2025 LogCenter. All rights reserved.