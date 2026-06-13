package com.chobolevel.logging.core;

import ch.qos.logback.classic.Level;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class LogLevelTest {

    @ParameterizedTest
    @MethodSource("levelMappings")
    void from_mapsLogbackLevelCorrectly(Level logbackLevel, LogLevel expected) {
        assertThat(LogLevel.from(logbackLevel)).isEqualTo(expected);
    }

    static Stream<Arguments> levelMappings() {
        return Stream.of(
                Arguments.of(Level.TRACE, LogLevel.TRACE),
                Arguments.of(Level.DEBUG, LogLevel.DEBUG),
                Arguments.of(Level.INFO,  LogLevel.INFO),
                Arguments.of(Level.WARN,  LogLevel.WARN),
                Arguments.of(Level.ERROR, LogLevel.ERROR)
        );
    }

    @Test
    void from_unknownLevel_returnsInfo() {
        assertThat(LogLevel.from(Level.ALL)).isEqualTo(LogLevel.INFO);
    }
}