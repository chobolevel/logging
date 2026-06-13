package com.chobolevel.logging.encoder;

import com.chobolevel.logging.core.LogRecord;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class PlainTextEncoder implements LogEncoder {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

    @Override
    public String encode(LogRecord record) {
        StringBuilder sb = new StringBuilder()
                .append(FMT.format(record.getTimestamp()))
                .append("  ").append(String.format("%-5s", record.getLevel()))
                .append(" [").append(record.getThreadName()).append("]");

        String traceId = record.getMdc() != null ? record.getMdc().get("traceId") : null;
        if (traceId != null) {
            sb.append(" [").append(traceId).append("]");
        }

        sb.append(" ").append(record.getLoggerName())
                .append(" - ").append(record.getMessage());

        if (record.getThrowableMessage() != null) {
            sb.append("\n  ").append(record.getThrowableMessage());
        }
        if (record.getThrowableStackTrace() != null) {
            sb.append("\n").append(record.getThrowableStackTrace());
        }

        return sb.toString();
    }
}