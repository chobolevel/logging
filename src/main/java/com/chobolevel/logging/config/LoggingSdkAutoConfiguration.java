package com.chobolevel.logging.config;

import ch.qos.logback.classic.LoggerContext;
import com.chobolevel.logging.appender.AsyncBufferedAppender;
import com.chobolevel.logging.appender.ConsoleAppender;
import com.chobolevel.logging.appender.FileRollingAppender;
import com.chobolevel.logging.appender.LogAppender;
import com.chobolevel.logging.encoder.JsonEncoder;
import com.chobolevel.logging.encoder.LogEncoder;
import com.chobolevel.logging.encoder.PlainTextEncoder;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.nio.file.Paths;

@AutoConfiguration
@EnableConfigurationProperties(LoggingSdkProperties.class)
@Import(WebFilterConfiguration.class)
public class LoggingSdkAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public LogEncoder logEncoder(LoggingSdkProperties props) {
        return switch (props.getEncoder()) {
            case JSON -> new JsonEncoder();
            case PLAIN -> new PlainTextEncoder();
        };
    }

    @Bean
    @ConditionalOnMissingBean
    public LogAppender logAppender(LoggingSdkProperties props) {
        LogAppender base;
        if (props.getFile().isEnabled()) {
            base = new FileRollingAppender(
                    Paths.get(props.getFile().getDirectory()),
                    props.getFile().getPattern()
            );
        } else {
            base = new ConsoleAppender();
        }

        if (props.isAsync()) {
            AsyncBufferedAppender asyncAppender = new AsyncBufferedAppender(base, props.getAsyncQueueSize());
            asyncAppender.start(); // internally calls delegate.start()
            return asyncAppender;
        }

        base.start();
        return base;
    }

    @Bean
    public SdkLogbackAppender sdkLogbackAppender(LogEncoder logEncoder, LogAppender logAppender) {
        LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger rootLogger = ctx.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);

        rootLogger.detachAppender("LOGGING_SDK");

        SdkLogbackAppender appender = new SdkLogbackAppender(logEncoder, logAppender);
        appender.setName("LOGGING_SDK");
        appender.setContext(ctx);
        appender.start();
        rootLogger.addAppender(appender);
        return appender;
    }
}