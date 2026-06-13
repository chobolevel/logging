package com.chobolevel.logging.config;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.AppenderBase;
import com.chobolevel.logging.appender.LogAppender;
import com.chobolevel.logging.core.LogLevel;
import com.chobolevel.logging.core.LogRecord;
import com.chobolevel.logging.encoder.LogEncoder;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

// DisposableBean: 빈 소멸 시점에 특정 동작을 하고 싶은 경우 구현하는 인터페이스
public class SdkLogbackAppender extends AppenderBase<ILoggingEvent> implements DisposableBean {

    private final LogEncoder logEncoder;
    private final LogAppender logAppender;

    public SdkLogbackAppender(LogEncoder logEncoder, LogAppender logAppender) {
        this.logEncoder = logEncoder;
        this.logAppender = logAppender;
    }

    @Override
    protected void append(ILoggingEvent event) {
        LogRecord record = toLogRecord(event);
        String encoded = logEncoder.encode(record);
        logAppender.append(encoded);
    }

    @Override
    public void destroy() {
        stop();
        LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
        ctx.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).detachAppender(getName());
    }

    private LogRecord toLogRecord(ILoggingEvent event) {
        LogRecord.LogRecordBuilder builder = LogRecord.builder()
                .timestamp(Instant.ofEpochMilli(event.getTimeStamp()))
                .level(LogLevel.from(event.getLevel()))
                .loggerName(event.getLoggerName())
                .threadName(event.getThreadName())
                .message(event.getFormattedMessage())
                .mdc(copyMdc(event.getMDCPropertyMap()));

        IThrowableProxy throwable = event.getThrowableProxy();
        if (throwable != null) {
            builder.throwableMessage(throwable.getClassName() + ": " + throwable.getMessage());
            builder.throwableStackTrace(formatStackTrace(throwable));
        }

        return builder.build();
    }

    private Map<String, String> copyMdc(Map<String, String> mdc) {
        if (mdc == null || mdc.isEmpty()) return Map.of();
        return new HashMap<>(mdc);
    }

    private String formatStackTrace(IThrowableProxy throwable) {
        StackTraceElementProxy[] elements = throwable.getStackTraceElementProxyArray();
        if (elements == null) return "";
        StringJoiner joiner = new StringJoiner("\n");
        int limit = Math.min(elements.length, 20);
        for (int i = 0; i < limit; i++) {
            joiner.add("\tat " + elements[i].getSTEAsString());
        }
        return joiner.toString();
    }
}