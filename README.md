# Logging SDK

[![](https://jitpack.io/v/chobolevel/logging.svg)](https://jitpack.io/#chobolevel/logging)

Spring Boot 애플리케이션에 의존성 하나로 끼워 넣을 수 있는 로깅 라이브러리.  
기존 `@Slf4j log.info()` 코드를 건드리지 않고 Logback 브릿지로 동작한다.

---

## 배경

Spring Boot가 기본 제공하는 Logback 설정은 `logback-spring.xml`을 직접 관리해야 하고,
JSON 포맷이나 비동기 처리, traceId 전파 같은 기능을 추가할 때마다 반복 작업이 생긴다.
이 SDK는 그 설정을 `application.yml` 프로퍼티 몇 줄로 대체하고, 확장이 필요할 때는
인터페이스를 Bean으로 등록하면 되도록 설계했다.

---

## 기능

- JSON / PlainText 인코더 (전환 가능)
- 날짜별 파일 롤링 (`ReentrantLock` 기반 동시성 보호)
- `BlockingQueue` 기반 비동기 버퍼 (드롭 카운터 포함)
- 콘솔 + 파일 동시 출력 (`CompositeAppender`)
- HTTP 요청마다 traceId 자동 주입 / 응답 헤더 전파 (`MdcTraceFilter`)
- Slack 웹훅 알림 (레벨 필터링, 비동기 전송)
- `@ConditionalOnMissingBean` — 인코더·appender 커스텀 교체 가능
- Spring Boot AutoConfiguration — `application.yml`만으로 설정 완료

---

## 아키텍처

```
@Slf4j log.info("msg")
        │
        ▼  ILoggingEvent
┌──────────────────────┐
│  SdkLogbackAppender  │  Logback 브릿지. ILoggingEvent → LogRecord 변환 후 아래 두 경로로 분기.
└──────────┬───────────┘
           │
     ┌─────┴──────────────────────────────────────────┐
     │ 인코딩 파이프라인                               │ 알림 파이프라인
     │                                                │
     ▼                                                ▼
 LogEncoder                                     AlertAppender
 (JSON / PlainText)                         (SlackWebhookAppender)
     │                                           레벨 필터링 후 비동기 HTTP
     ▼
 LogAppender
 ├── ConsoleAppender
 ├── FileRollingAppender
 └── CompositeAppender (둘 동시)
      └── AsyncBufferedAppender (데코레이터)
```

두 파이프라인을 분리한 이유: Slack 같은 알림 채널은 인코딩된 문자열이 아닌
`LogRecord` 원본이 필요하다 (레벨 필터링, 필드별 포맷팅).

---

## 설치

### JitPack (권장)

별도 빌드 없이 JitPack 저장소를 추가하고 의존성을 선언하면 끝난다.

**Gradle**

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.chobolevel:logging-sdk:v1.0.0'
}
```

**Maven**

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.chobolevel</groupId>
    <artifactId>logging-sdk</artifactId>
    <version>v1.0.0</version>
</dependency>
```

> JitPack 페이지: https://jitpack.io/#chobolevel/logging/v1.0.0

---

## 설정

```yaml
logging-sdk:
  encoder: PLAIN               # PLAIN(기본) | JSON
  async: false                 # BlockingQueue 기반 비동기 처리
  async-queue-size: 1000       # 큐 초과 시 drop (드롭 카운터 제공)

  console:
    enabled: true              # 콘솔 출력 (기본 true)

  file:
    enabled: false             # 파일 출력
    directory: logs
    pattern: "app-%s.log"      # %s → yyyy-MM-dd

  slack:
    enabled: false             # Slack 웹훅 알림
    webhook-url: https://hooks.slack.com/services/...
    min-level: ERROR           # 이 레벨 이상만 전송 (기본 ERROR)
```

| 환경 | 설정 |
|---|---|
| 로컬 개발 | 기본값 |
| 운영 (JSON + 파일 + 비동기) | `encoder: JSON` + `async: true` + `file.enabled: true` |
| 운영 + Slack 에러 알림 | 위 설정 + `slack.enabled: true` + `slack.webhook-url: ...` |

---

## 사용

의존성 추가 후 코드 변경 없이 동작한다.

```java
@Slf4j
@Service
public class OrderService {
    public void create() {
        log.info("주문 생성");
        log.error("결제 실패", ex);  // throwableMessage / stackTrace 자동 포함
    }
}
```

**커스텀 인코더 교체** — `LogEncoder` Bean을 등록하면 기본 인코더보다 우선 적용.

```java
@Bean
public LogEncoder logEncoder() {
    return record -> String.format("[%s] %s - %s",
        record.getMdc().getOrDefault("traceId", "-"),
        record.getLevel(),
        record.getMessage()
    );
}
```

**커스텀 Appender 교체** — `LogAppender` Bean을 등록하면 기본 appender보다 우선 적용.

```java
@Bean
public LogAppender logAppender() {
    return encoded -> kafkaTemplate.send("logs", encoded);
}
```

---

## 프로젝트 구조

```
src/main/java/com/chobolevel/logging/
├── core/
│   ├── LogLevel.java               Logback Level → SDK Level 변환
│   └── LogRecord.java              불변 로그 데이터 모델
│
├── encoder/
│   ├── LogEncoder.java             인터페이스 (확장 포인트)
│   ├── JsonEncoder.java
│   └── PlainTextEncoder.java
│
├── appender/
│   ├── LogAppender.java            인터페이스 (확장 포인트)
│   ├── AlertAppender.java          알림 채널 인터페이스 (LogRecord 수신)
│   ├── ConsoleAppender.java
│   ├── FileRollingAppender.java    날짜별 롤링, ReentrantLock
│   ├── AsyncBufferedAppender.java  BlockingQueue 데코레이터, 드롭 카운터
│   ├── CompositeAppender.java      여러 appender 동시 출력
│   └── SlackWebhookAppender.java   레벨 필터링 + 비동기 HTTP
│
├── filter/
│   └── MdcTraceFilter.java         X-Trace-Id 헤더 기반 traceId MDC 주입
│
└── config/
    ├── SdkLogbackAppender.java         Logback 브릿지
    ├── LoggingSdkProperties.java       프로퍼티 바인딩
    ├── WebFilterConfiguration.java     웹 환경 조건부 필터 등록
    └── LoggingSdkAutoConfiguration.java
```

---

## 설계 결정

**브릿지 패턴** — Logback 종속 코드를 `SdkLogbackAppender` 한 곳에만 격리했다.
`LogEncoder`와 `LogAppender`는 Logback을 전혀 모르기 때문에 Logback이 아닌 다른
프레임워크와 연결할 때도 브릿지만 교체하면 된다.

**인코딩·알림 파이프라인 분리** — 파일/콘솔은 인코딩된 String을 소비하면 충분하지만,
Slack은 레벨 기반 색상 코딩과 필드별 포맷팅을 위해 `LogRecord` 원본이 필요하다.
두 경로를 `SdkLogbackAppender`에서 분기함으로써 각 채널이 필요한 데이터만 받는다.

**데코레이터 패턴** — `AsyncBufferedAppender`는 어떤 `LogAppender`든 감쌀 수 있다.
`CompositeAppender` 역시 `LogAppender` 목록을 받아 투명하게 위임한다.
두 조합을 중첩할 수도 있다: `Async(Composite(Console, File))`.

**드롭 카운터** — 비동기 큐가 가득 찼을 때 호출 스레드를 블로킹하는 대신 drop한다.
drop 수는 `getDroppedCount()`로 노출되어 있어 Micrometer 등으로 모니터링할 수 있다.

**정상 종료** — `DisposableBean` 구현으로 Spring 컨텍스트 종료 시 큐에 남은 항목을
모두 flush한 뒤 파일 핸들과 스레드를 종료한다.