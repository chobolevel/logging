package com.chobolevel.logging.appender;

import com.chobolevel.logging.core.LogRecord;

public interface AlertAppender {
    void send(LogRecord record);
    default void start() {}
    default void stop() {}
}