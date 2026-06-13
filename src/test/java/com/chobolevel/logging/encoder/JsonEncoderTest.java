package com.chobolevel.logging.encoder;

import com.chobolevel.logging.core.LogLevel;
import com.chobolevel.logging.core.LogRecord;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JsonEncoderTest {

    private JsonEncoder encoder;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        encoder = new JsonEncoder();
    }

    @Test
    void encode_basicRecord_producesValidJson() throws Exception {
        LogRecord record = LogRecord.builder()
                .timestamp(Instant.parse("2024-01-15T10:00:00Z"))
                .level(LogLevel.INFO)
                .loggerName("com.example.Foo")
                .threadName("main")
                .message("hello world")
                .mdc(Map.of())
                .build();

        String json = encoder.encode(record);
        JsonNode node = mapper.readTree(json);

        assertThat(node.get("level").asText()).isEqualTo("INFO");
        assertThat(node.get("message").asText()).isEqualTo("hello world");
        assertThat(node.get("logger").asText()).isEqualTo("com.example.Foo");
        assertThat(node.get("thread").asText()).isEqualTo("main");
        assertThat(node.has("mdc")).isFalse();
    }

    @Test
    void encode_withMdc_includesMdc() throws Exception {
        LogRecord record = LogRecord.builder()
                .timestamp(Instant.now())
                .level(LogLevel.DEBUG)
                .loggerName("test")
                .threadName("t1")
                .message("msg")
                .mdc(Map.of("traceId", "abc-123"))
                .build();

        JsonNode node = mapper.readTree(encoder.encode(record));

        assertThat(node.get("mdc").get("traceId").asText()).isEqualTo("abc-123");
    }

    @Test
    void encode_withThrowable_includesThrowableFields() throws Exception {
        LogRecord record = LogRecord.builder()
                .timestamp(Instant.now())
                .level(LogLevel.ERROR)
                .loggerName("test")
                .threadName("t1")
                .message("error")
                .mdc(Map.of())
                .throwableMessage("java.lang.RuntimeException: oops")
                .throwableStackTrace("\tat com.example.Foo.bar(Foo.java:42)")
                .build();

        JsonNode node = mapper.readTree(encoder.encode(record));

        assertThat(node.get("throwable").asText()).isEqualTo("java.lang.RuntimeException: oops");
        assertThat(node.get("stackTrace").asText()).contains("Foo.java:42");
    }
}