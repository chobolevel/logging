package com.chobolevel.logging.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import com.chobolevel.logging.appender.LogAppender;
import com.chobolevel.logging.core.LogLevel;
import com.chobolevel.logging.core.LogRecord;
import com.chobolevel.logging.encoder.LogEncoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SdkLogbackAppenderTest {

    private List<LogRecord> capturedRecords;
    private List<String> capturedEncoded;
    private SdkLogbackAppender appender;

    @BeforeEach
    void setUp() {
        capturedRecords = new ArrayList<>();
        capturedEncoded = new ArrayList<>();

        LogEncoder encoder = record -> {
            capturedRecords.add(record);
            return "encoded:" + record.getMessage();
        };
        LogAppender logAppender = encoded -> capturedEncoded.add(encoded);

        LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
        appender = new SdkLogbackAppender(encoder, logAppender, List.of());
        appender.setName("TEST_SDK");
        appender.setContext(ctx);
        appender.start();
    }

    @Test
    void append_convertsLoggingEventToLogRecord() {
        LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger logger = ctx.getLogger("com.example.Test");

        LoggingEvent event = new LoggingEvent(
                "com.example.Test",
                logger,
                Level.WARN,
                "test warning",
                null,
                null
        );

        appender.doAppend(event);

        assertThat(capturedRecords).hasSize(1);
        LogRecord record = capturedRecords.get(0);
        assertThat(record.getLevel()).isEqualTo(LogLevel.WARN);
        assertThat(record.getMessage()).isEqualTo("test warning");
        assertThat(record.getLoggerName()).isEqualTo("com.example.Test");
    }

    @Test
    void append_encodedStringPassedToLogAppender() {
        LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger logger = ctx.getLogger("test");

        LoggingEvent event = new LoggingEvent("test", logger, Level.INFO, "hello", null, null);
        appender.doAppend(event);

        assertThat(capturedEncoded).containsExactly("encoded:hello");
    }
}