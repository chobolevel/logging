package com.chobolevel.logging.appender;

public class ConsoleAppender implements LogAppender {

    @Override
    public void append(String encoded) {
        System.out.println(encoded);
    }
}