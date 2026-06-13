package com.chobolevel.logging.appender;

import org.springframework.beans.factory.DisposableBean;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.concurrent.locks.ReentrantLock;

public class FileRollingAppender implements LogAppender, DisposableBean {

    private final Path logDirectory;
    private final String filenamePattern;
    private final ReentrantLock lock = new ReentrantLock();

    private volatile LocalDate currentDate;
    private volatile BufferedWriter currentWriter;

    public FileRollingAppender(Path logDirectory, String filenamePattern) {
        this.logDirectory = logDirectory;
        this.filenamePattern = filenamePattern;
    }

    @Override
    public void start() {
        lock.lock();
        try {
            currentDate = LocalDate.now();
            currentWriter = openWriter(currentDate);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to open log file", e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void stop() {
        lock.lock();
        try {
            if (currentWriter != null) {
                currentWriter.close();
                currentWriter = null;
            }
        } catch (IOException e) {
            // ignore on shutdown
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void destroy() {
        stop();
    }

    @Override
    public void append(String encoded) {
        lock.lock();
        try {
            LocalDate today = LocalDate.now();
            if (!today.equals(currentDate)) {
                rotate(today);
            }
            if (currentWriter != null) {
                currentWriter.write(encoded);
                currentWriter.newLine();
                currentWriter.flush();
            }
        } catch (IOException e) {
            System.err.println("[logging-sdk] FileRollingAppender write failed: " + e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    private void rotate(LocalDate newDate) throws IOException {
        if (currentWriter != null) {
            currentWriter.close();
        }
        currentDate = newDate;
        currentWriter = openWriter(newDate);
    }

    private BufferedWriter openWriter(LocalDate date) throws IOException {
        Files.createDirectories(logDirectory);
        Path file = logDirectory.resolve(String.format(filenamePattern, date));
        return Files.newBufferedWriter(file, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
}