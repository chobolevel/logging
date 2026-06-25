package com.chobolevel.logging.appender;

import java.util.List;

public class CompositeAppender implements LogAppender {

    private final List<LogAppender> appenders;

    public CompositeAppender(List<LogAppender> appenders) {
        this.appenders = List.copyOf(appenders);
    }

    @Override
    public void start() {
        appenders.forEach(LogAppender::start);
    }

    @Override
    public void stop() {
        appenders.forEach(LogAppender::stop);
    }

    @Override
    public void append(String encoded) {
        appenders.forEach(a -> a.append(encoded));
    }
}