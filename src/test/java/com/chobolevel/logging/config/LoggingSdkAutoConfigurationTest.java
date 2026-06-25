package com.chobolevel.logging.config;

import com.chobolevel.logging.appender.AsyncBufferedAppender;
import com.chobolevel.logging.appender.CompositeAppender;
import com.chobolevel.logging.appender.ConsoleAppender;
import com.chobolevel.logging.appender.LogAppender;
import com.chobolevel.logging.encoder.JsonEncoder;
import com.chobolevel.logging.encoder.LogEncoder;
import com.chobolevel.logging.encoder.PlainTextEncoder;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class LoggingSdkAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(LoggingSdkAutoConfiguration.class));

    @Test
    void defaultConfig_registersPlainTextEncoderAndConsoleAppender() {
        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(LogEncoder.class);
            assertThat(ctx).hasSingleBean(LogAppender.class);
            assertThat(ctx.getBean(LogEncoder.class)).isInstanceOf(PlainTextEncoder.class);
            assertThat(ctx.getBean(LogAppender.class)).isInstanceOf(ConsoleAppender.class);
        });
    }

    @Test
    void jsonEncoderProperty_registersJsonEncoder() {
        runner.withPropertyValues("logging-sdk.encoder=JSON")
                .run(ctx -> assertThat(ctx.getBean(LogEncoder.class)).isInstanceOf(JsonEncoder.class));
    }

    @Test
    void asyncProperty_wrapsAppenderInAsync() {
        runner.withPropertyValues("logging-sdk.async=true", "logging-sdk.async-queue-size=500")
                .run(ctx -> assertThat(ctx.getBean(LogAppender.class)).isInstanceOf(AsyncBufferedAppender.class));
    }

    @Test
    void customLogEncoder_takesOverAutoConfigured() {
        runner.withUserConfiguration(CustomEncoderConfig.class)
                .run(ctx -> {
                    assertThat(ctx.getBean(LogEncoder.class)).isInstanceOf(CustomEncoderConfig.CustomEncoder.class);
                });
    }

    @Test
    void customLogAppender_takesOverAutoConfigured() {
        runner.withUserConfiguration(CustomAppenderConfig.class)
                .run(ctx -> {
                    assertThat(ctx.getBean(LogAppender.class)).isInstanceOf(CustomAppenderConfig.CustomAppender.class);
                });
    }

    @Test
    void consoleAndFileEnabled_wrapsInCompositeAppender() {
        runner.withPropertyValues(
                "logging-sdk.console.enabled=true",
                "logging-sdk.file.enabled=true",
                "logging-sdk.file.directory=/tmp/logging-sdk-test"
        ).run(ctx -> assertThat(ctx.getBean(LogAppender.class)).isInstanceOf(CompositeAppender.class));
    }

    @Test
    void consoleDisabled_fileEnabled_returnsSingleAppender() {
        runner.withPropertyValues(
                "logging-sdk.console.enabled=false",
                "logging-sdk.file.enabled=true",
                "logging-sdk.file.directory=/tmp/logging-sdk-test"
        ).run(ctx -> assertThat(ctx.getBean(LogAppender.class)).isNotInstanceOf(CompositeAppender.class));
    }

    @Test
    void sdkLogbackAppender_alwaysRegistered() {
        runner.run(ctx -> assertThat(ctx).hasSingleBean(SdkLogbackAppender.class));
    }

    @Configuration
    static class CustomEncoderConfig {
        @Bean
        LogEncoder logEncoder() { return new CustomEncoder(); }

        static class CustomEncoder implements LogEncoder {
            @Override
            public String encode(com.chobolevel.logging.core.LogRecord record) {
                return "custom:" + record.getMessage();
            }
        }
    }

    @Configuration
    static class CustomAppenderConfig {
        @Bean
        LogAppender logAppender() { return new CustomAppender(); }

        static class CustomAppender implements LogAppender {
            @Override
            public void append(String encoded) {}
        }
    }
}