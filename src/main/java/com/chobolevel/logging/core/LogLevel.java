package com.chobolevel.logging.core;

import ch.qos.logback.classic.Level;

public enum LogLevel {
    TRACE, DEBUG, INFO, WARN, ERROR;

    public static LogLevel from(Level level) {
        if (level == Level.TRACE) return TRACE;
        if (level == Level.DEBUG) return DEBUG;
        if (level == Level.WARN)  return WARN;
        if (level == Level.ERROR) return ERROR;
        return INFO;
    }
}