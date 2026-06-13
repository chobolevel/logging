package com.chobolevel.logging.appender;

import org.springframework.beans.factory.DisposableBean;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class AsyncBufferedAppender implements LogAppender, DisposableBean {

    private final LogAppender delegate;
    private final BlockingQueue<String> queue;
    private final ExecutorService executor;
    private volatile boolean running = false;

    public AsyncBufferedAppender(LogAppender delegate, int queueSize) {
        this.delegate = delegate;
        this.queue = new LinkedBlockingQueue<>(queueSize);
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "logging-sdk-async");
            t.setDaemon(true);
            return t;
        });
    }

    @Override
    public void start() {
        running = true;
        delegate.start();
        executor.submit(this::drainLoop);
    }

    @Override
    public void stop() {
        running = false;
        executor.shutdown();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        String item;
        while ((item = queue.poll()) != null) {
            delegate.append(item);
        }
        delegate.stop();
    }

    @Override
    public void destroy() {
        stop();
    }

    @Override
    public void append(String encoded) {
        if (!queue.offer(encoded)) {
            // queue full — drop to prevent backpressure on caller
        }
    }

    private void drainLoop() {
        while (running) {
            try {
                String encoded = queue.poll(100, TimeUnit.MILLISECONDS);
                if (encoded != null) {
                    delegate.append(encoded);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}