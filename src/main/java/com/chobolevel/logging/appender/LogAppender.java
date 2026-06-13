package com.chobolevel.logging.appender;

public interface LogAppender {
    void append(String encoded);

    default void start() {}

    default void stop() {}
}