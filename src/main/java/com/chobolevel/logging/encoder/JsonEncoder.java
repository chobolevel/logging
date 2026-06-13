package com.chobolevel.logging.encoder;

import com.chobolevel.logging.core.LogRecord;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

public class JsonEncoder implements LogEncoder {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String encode(LogRecord record) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("timestamp", record.getTimestamp().toString());
        map.put("level", record.getLevel().name());
        map.put("logger", record.getLoggerName());
        map.put("thread", record.getThreadName());
        map.put("message", record.getMessage());

        if (record.getMdc() != null && !record.getMdc().isEmpty()) {
            map.put("mdc", record.getMdc());
        }
        if (record.getThrowableMessage() != null) {
            map.put("throwable", record.getThrowableMessage());
        }
        if (record.getThrowableStackTrace() != null) {
            map.put("stackTrace", record.getThrowableStackTrace());
        }

        try {
            return mapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            return "{\"error\":\"encoding failed\",\"message\":\"" + record.getMessage() + "\"}";
        }
    }
}