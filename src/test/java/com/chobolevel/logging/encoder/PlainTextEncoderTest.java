package com.chobolevel.logging.encoder;

import com.chobolevel.logging.core.LogLevel;
import com.chobolevel.logging.core.LogRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PlainTextEncoderTest {

    private PlainTextEncoder encoder;

    @BeforeEach
    void setUp() {
        encoder = new PlainTextEncoder();
    }

    @Test
    void encode_basicRecord_containsRequiredFields() {
        LogRecord record = LogRecord.builder()
                .timestamp(Instant.now())
                .level(LogLevel.INFO)
                .loggerName("com.example.Foo")
                .threadName("main")
                .message("test message")
                .mdc(Map.of())
                .build();

        String result = encoder.encode(record);

        assertThat(result).contains("INFO");
        assertThat(result).contains("[main]");
        assertThat(result).contains("com.example.Foo");
        assertThat(result).contains("test message");
    }

    @Test
    void encode_withTraceId_includesTraceIdBracket() {
        LogRecord record = LogRecord.builder()
                .timestamp(Instant.now())
                .level(LogLevel.WARN)
                .loggerName("com.example.Bar")
                .threadName("http-1")
                .message("warn msg")
                .mdc(Map.of("traceId", "trace-xyz"))
                .build();

        String result = encoder.encode(record);

        assertThat(result).contains("[trace-xyz]");
    }

    @Test
    void encode_withThrowable_appendsOnNewLine() {
        LogRecord record = LogRecord.builder()
                .timestamp(Instant.now())
                .level(LogLevel.ERROR)
                .loggerName("com.example.Svc")
                .threadName("worker")
                .message("failed")
                .mdc(Map.of())
                .throwableMessage("java.lang.NullPointerException: null ref")
                .build();

        String result = encoder.encode(record);

        assertThat(result).contains("\n");
        assertThat(result).contains("NullPointerException");
    }
}