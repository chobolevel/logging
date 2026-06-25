package com.chobolevel.logging.appender;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CompositeAppenderTest {

    @Test
    void append_delegatesToAllAppenders() {
        List<String> first = new ArrayList<>();
        List<String> second = new ArrayList<>();

        CompositeAppender composite = new CompositeAppender(List.of(
                first::add,
                second::add
        ));

        composite.append("msg1");
        composite.append("msg2");

        assertThat(first).containsExactly("msg1", "msg2");
        assertThat(second).containsExactly("msg1", "msg2");
    }

    @Test
    void start_andStop_invokesAllDelegates() {
        List<String> events = new ArrayList<>();

        LogAppender a = new LogAppender() {
            @Override public void start() { events.add("a:start"); }
            @Override public void stop()  { events.add("a:stop"); }
            @Override public void append(String e) {}
        };
        LogAppender b = new LogAppender() {
            @Override public void start() { events.add("b:start"); }
            @Override public void stop()  { events.add("b:stop"); }
            @Override public void append(String e) {}
        };

        CompositeAppender composite = new CompositeAppender(List.of(a, b));
        composite.start();
        composite.stop();

        assertThat(events).containsExactly("a:start", "b:start", "a:stop", "b:stop");
    }
}