# Logging SDK

Java 17 / Spring Boot 3.2 기반 커스텀 로깅 SDK.  
`@Slf4j log.info()` 코드를 수정하지 않고 Logback 브릿지 구조로 동작하며, JSON/PlainText 인코더, 비동기 버퍼, 날짜별 파일 롤링, MDC traceId 자동 주입을 제공합니다.

---

## 기술 스택

- Java 17
- Spring Boot 3.2
- Logback (logback-classic)
- Gradle

---

## 프로젝트 구조

```
src/main/java/com/chobolevel/logging/
├── core/
│   ├── LogLevel.java              # 로그 레벨 Enum (Logback Level → SDK Level 변환)
│   └── LogRecord.java             # 로그 데이터 불변 모델
│
├── encoder/
│   ├── LogEncoder.java            # 포맷 인터페이스 (사용측 확장 포인트)
│   ├── JsonEncoder.java           # JSON 포맷 구현체
│   └── PlainTextEncoder.java      # 텍스트 포맷 구현체
│
├── appender/
│   ├── LogAppender.java           # 출력 인터페이스 (사용측 확장 포인트)
│   ├── ConsoleAppender.java       # 콘솔 출력
│   ├── AsyncBufferedAppender.java # BlockingQueue 기반 비동기 래퍼
│   └── FileRollingAppender.java   # 날짜별 롤링 파일 출력
│
├── filter/
│   └── MdcTraceFilter.java        # X-Trace-Id 헤더 기반 traceId MDC 자동 주입
│
└── config/
    ├── SdkLogbackAppender.java          # Logback ILoggingEvent → LogRecord 변환 브릿지
    ├── LoggingSdkProperties.java        # application.yml 프로퍼티 바인딩
    ├── WebFilterConfiguration.java      # 웹 환경 조건부 필터 설정
    └── LoggingSdkAutoConfiguration.java # Spring Boot AutoConfiguration
```

---

## 동작 흐름

### 전체 흐름

```
@Slf4j
log.info("주문 생성")
        │
        ▼
┌─────────────────────┐
│      SLF4J API      │  ← 코드가 직접 호출하는 인터페이스
└─────────────────────┘
        │
        ▼
┌─────────────────────┐
│      Logback        │  ← SLF4J 구현체, ILoggingEvent 생성
│  (logback-classic)  │    (시간, 레벨, 메시지, MDC, 스레드명 등 담김)
└─────────────────────┘
        │  ILoggingEvent
        ▼
┌─────────────────────┐
│  SdkLogbackAppender │  ← AppenderBase 확장, Logback이 여기로 이벤트 전달
│       (브릿지)       │    Spring 컨텍스트 로드 시 root logger에 자동 등록
└─────────────────────┘
        │  ILoggingEvent → LogRecord 변환
        ▼
┌─────────────────────┐
│      LogRecord      │  ← 불변 데이터 모델
│   (불변 도메인 모델) │    timestamp / level / loggerName
│                     │    threadName / message / mdc
│                     │    throwableMessage / throwableStackTrace
└─────────────────────┘
        │
        ▼
┌─────────────────────┐
│     LogEncoder      │  ← encode(LogRecord) → String
│  PlainTextEncoder   │
│    JsonEncoder      │
└─────────────────────┘
        │  "2024-01-01 INFO [main] ..."
        ▼
┌──────────────────────────────────────────┐
│              LogAppender                 │
│                                          │
│  async=false → ConsoleAppender           │  → System.out.println()
│                FileRollingAppender       │  → 날짜별 파일 쓰기
│                                          │
│  async=true  → AsyncBufferedAppender     │  → BlockingQueue에 enqueue
│                    │                     │
│                    └─ 별도 스레드(daemon) │  → delegate.append() 비동기 처리
└──────────────────────────────────────────┘
```

### Spring 컨텍스트 초기화 (앱 시작 시)

```
Spring Boot 시작
      │
      ▼
LoggingSdkAutoConfiguration 로드
      │
      ├── LoggingSdkProperties 바인딩
      │     application.yml → logging-sdk.encoder / async / file ...
      │
      ├── LogEncoder Bean 생성  (@ConditionalOnMissingBean)
      │     encoder=PLAIN → PlainTextEncoder
      │     encoder=JSON  → JsonEncoder
      │     사용자 Bean 등록 시 → 사용자 것 우선 적용
      │
      ├── LogAppender Bean 생성  (@ConditionalOnMissingBean)
      │     file.enabled=false → ConsoleAppender
      │     file.enabled=true  → FileRollingAppender (start() 호출)
      │     async=true         → AsyncBufferedAppender로 래핑 (전용 스레드 기동)
      │     사용자 Bean 등록 시 → 사용자 것 우선 적용
      │
      ├── SdkLogbackAppender Bean 생성
      │     Logback root logger에 "LOGGING_SDK" 이름으로 appender 등록
      │     → 이 시점부터 모든 log.xxx() 호출이 SDK로 흘러들어옴
      │
      └── MdcTraceFilter Bean 생성  (WebApplication 환경일 때만)
            서블릿 필터 체인에 자동 등록
```

### HTTP 요청 처리 (웹 앱일 때)

```
HTTP Request (X-Trace-Id: abc-123)
      │
      ▼
MdcTraceFilter
      │  MDC.put("traceId", "abc-123")  ← 헤더 없으면 UUID 자동 생성
      │
      ▼
Controller → Service → Repository
      │
      │  log.info("처리 완료")
      │       ↓ ILoggingEvent (MDC에 traceId 자동 포함)
      │       ↓ LogRecord.mdc = {"traceId": "abc-123"}
      │       ↓
      │  PlainText: "2024-01-01 INFO  [http-1] [abc-123] com.Foo - 처리 완료"
      │  JSON:      {"timestamp":"...","level":"INFO","mdc":{"traceId":"abc-123"},...}
      │
      ▼
HTTP Response (X-Trace-Id: abc-123 헤더 자동 포함)
      │
      ▼
MdcTraceFilter (finally)
      │  MDC.remove("traceId")  ← 요청 종료 후 반드시 정리
```

### 컨텍스트 종료 시

```
Spring shutdown
      │
      ├── SdkLogbackAppender.destroy()
      │     AppenderBase.stop() → Logback root logger에서 detach
      │
      └── LogAppender.destroy()  (DisposableBean)
            AsyncBufferedAppender → 큐에 남은 항목 flush 후 스레드 종료
            FileRollingAppender  → BufferedWriter 정상 close
```

---

## 설치

### Maven Local 퍼블리시

```bash
./gradlew publishToMavenLocal
```

### 소비 프로젝트 build.gradle

```groovy
repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation 'com.chobolevel:logging-sdk:1.0.0'
}
```

---

## 설정

`application.yml`에 아래 프로퍼티를 추가합니다. 모든 항목은 기본값이 있어 설정 없이도 동작합니다.

```yaml
logging-sdk:
  encoder: PLAIN          # PLAIN(기본) | JSON
  async: false            # true 시 BlockingQueue 기반 비동기 처리
  async-queue-size: 1000  # 비동기 큐 크기 (기본 1000, 초과 시 drop)
  file:
    enabled: false        # true 시 파일 출력 (콘솔 대신)
    directory: logs       # 로그 파일 저장 디렉토리
    pattern: "app-%s.log" # 파일명 패턴 (%s 자리에 날짜 YYYY-MM-DD 치환)
```

### 설정 조합 예시

| 목적 | 설정 |
|---|---|
| 개발 환경 (콘솔, 텍스트) | 기본값 사용 |
| 운영 환경 (콘솔, JSON) | `encoder: JSON` |
| 운영 환경 (파일, JSON, 비동기) | `encoder: JSON` + `async: true` + `file.enabled: true` |

---

## 사용 방법

### 기본 사용 — 코드 수정 없음

```java
@Slf4j
@Service
public class OrderService {

    public void create() {
        log.info("주문 생성");        // PlainText: "2024-01-01 INFO  [main] com.example.OrderService - 주문 생성"
        log.warn("재고 부족");        // JSON:      {"level":"WARN","message":"재고 부족",...}
        log.error("결제 실패", ex);   // throwableMessage / stackTrace 자동 포함
    }
}
```

### 커스텀 LogEncoder 등록

`LogEncoder` Bean을 직접 등록하면 AutoConfiguration의 기본 인코더보다 우선 적용됩니다.

```java
@Bean
public LogEncoder logEncoder() {
    return record -> String.format("[%s] %s %s - %s",
        record.getMdc().getOrDefault("traceId", "-"),
        record.getLevel(),
        record.getLoggerName(),
        record.getMessage()
    );
}
```

### 커스텀 LogAppender 등록

`LogAppender` Bean을 직접 등록하면 AutoConfiguration의 기본 appender보다 우선 적용됩니다.

```java
@Bean
public LogAppender logAppender() {
    return encoded -> {
        // Kafka, Elasticsearch, 외부 수집기 등으로 전송
        kafkaTemplate.send("logs", encoded);
    };
}
```

---

## 핵심 설계 포인트

| 포인트 | 설명 |
|---|---|
| **코드 무침투** | `@Slf4j log.info()` 그대로 사용, 기존 코드 변경 없음 |
| **브릿지 패턴** | Logback `AppenderBase` 확장으로 SLF4J 생태계와 연결 |
| **@ConditionalOnMissingBean** | `LogEncoder` 또는 `LogAppender` Bean 등록 시 자동으로 사용자 구현체 우선 적용 |
| **비동기 격리** | `AsyncBufferedAppender`가 호출 스레드 블로킹 없이 큐 경유 처리, 큐 초과 시 drop으로 호출 스레드 보호 |
| **MDC 자동 주입** | 서블릿 필터가 요청 시작/종료를 감싸 traceId 생명주기 관리, 헤더 없으면 UUID 자동 생성 |
| **날짜별 롤링** | `FileRollingAppender`가 자정 기준으로 파일 자동 교체, `ReentrantLock`으로 동시성 보호 |
| **정상 종료** | `DisposableBean` 구현으로 Spring 컨텍스트 종료 시 큐 flush 및 파일 핸들 정상 close |