package com.chobolevel.logging.core;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.Map;

@Value
@Builder
public class LogRecord {
    Instant timestamp;
    LogLevel level;
    String loggerName;
    String threadName;
    String message;
    Map<String, String> mdc;
    String throwableMessage;
    String throwableStackTrace;
}