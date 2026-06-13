package com.chobolevel.logging.appender;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class AsyncBufferedAppenderTest {

    @Test
    void append_delegatesToUnderlyingAppender() throws InterruptedException {
        List<String> received = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(3);

        LogAppender delegate = encoded -> {
            received.add(encoded);
            latch.countDown();
        };

        AsyncBufferedAppender async = new AsyncBufferedAppender(delegate, 100);
        async.start();

        async.append("msg1");
        async.append("msg2");
        async.append("msg3");

        assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();
        assertThat(received).containsExactlyInAnyOrder("msg1", "msg2", "msg3");

        async.stop();
    }

    @Test
    void append_whenQueueFull_dropsWithoutException() {
        LogAppender blockingDelegate = encoded -> {
            try { Thread.sleep(50); } catch (InterruptedException ignored) {}
        };

        AsyncBufferedAppender async = new AsyncBufferedAppender(blockingDelegate, 2);
        async.start();

        // overflow queue — should not throw
        for (int i = 0; i < 10; i++) {
            async.append("msg" + i);
        }

        async.stop();
    }

    @Test
    void stop_flushesPendingMessages() throws InterruptedException {
        List<String> received = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(5);

        LogAppender delegate = encoded -> {
            received.add(encoded);
            latch.countDown();
        };

        AsyncBufferedAppender async = new AsyncBufferedAppender(delegate, 100);
        async.start();

        for (int i = 0; i < 5; i++) {
            async.append("item" + i);
        }

        async.stop();

        assertThat(received).hasSize(5);
    }
}