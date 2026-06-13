package com.chobolevel.logging.appender;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FileRollingAppenderTest {

    @TempDir
    Path tempDir;

    @Test
    void append_writesLinesToFile() throws Exception {
        FileRollingAppender appender = new FileRollingAppender(tempDir, "test-%s.log");
        appender.start();

        appender.append("line one");
        appender.append("line two");

        appender.stop();

        Path logFile = tempDir.resolve("test-" + LocalDate.now() + ".log");
        assertThat(logFile).exists();

        List<String> lines = Files.readAllLines(logFile);
        assertThat(lines).containsExactly("line one", "line two");
    }

    @Test
    void append_createsDirectoryIfAbsent() throws Exception {
        Path nested = tempDir.resolve("sub/dir");
        FileRollingAppender appender = new FileRollingAppender(nested, "app-%s.log");
        appender.start();

        appender.append("hello");
        appender.stop();

        assertThat(nested).isDirectory();
        assertThat(nested.resolve("app-" + LocalDate.now() + ".log")).exists();
    }

    @Test
    void append_rollsFileOnDateChange() throws Exception {
        FileRollingAppender appender = new FileRollingAppender(tempDir, "app-%s.log");
        appender.start();
        appender.append("today");

        // simulate date change by setting currentDate to yesterday
        LocalDate yesterday = LocalDate.now().minusDays(1);
        Field dateField = FileRollingAppender.class.getDeclaredField("currentDate");
        dateField.setAccessible(true);
        dateField.set(appender, yesterday);

        appender.append("after roll");
        appender.stop();

        Path todayFile = tempDir.resolve("app-" + LocalDate.now() + ".log");
        assertThat(todayFile).exists();
        assertThat(Files.readAllLines(todayFile)).contains("after roll");
    }
}