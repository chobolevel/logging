package com.chobolevel.logging.config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.chobolevel.logging.appender.AlertAppender;
import com.chobolevel.logging.appender.AsyncBufferedAppender;
import com.chobolevel.logging.appender.CompositeAppender;
import com.chobolevel.logging.appender.ConsoleAppender;
import com.chobolevel.logging.appender.FileRollingAppender;
import com.chobolevel.logging.appender.LogAppender;
import com.chobolevel.logging.appender.SlackWebhookAppender;
import com.chobolevel.logging.encoder.JsonEncoder;
import com.chobolevel.logging.encoder.LogEncoder;
import com.chobolevel.logging.encoder.PlainTextEncoder;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

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
        List<LogAppender> bases = new ArrayList<>();

        if (props.getConsole().isEnabled()) {
            bases.add(new ConsoleAppender());
        }
        if (props.getFile().isEnabled()) {
            bases.add(new FileRollingAppender(
                    Paths.get(props.getFile().getDirectory()),
                    props.getFile().getPattern()
            ));
        }
        if (bases.isEmpty()) {
            bases.add(new ConsoleAppender());
        }

        LogAppender base = bases.size() == 1 ? bases.get(0) : new CompositeAppender(bases);

        if (props.isAsync()) {
            AsyncBufferedAppender asyncAppender = new AsyncBufferedAppender(base, props.getAsyncQueueSize());
            asyncAppender.start();
            return asyncAppender;
        }

        base.start();
        return base;
    }

    @Bean
    @ConditionalOnProperty(prefix = "logging-sdk.slack", name = "enabled", havingValue = "true")
    public AlertAppender slackWebhookAppender(LoggingSdkProperties props) {
        SlackWebhookAppender appender = new SlackWebhookAppender(
                props.getSlack().getWebhookUrl(),
                props.getSlack().getMinLevel()
        );
        appender.start();
        return appender;
    }

    @Bean
    public SdkLogbackAppender sdkLogbackAppender(LogEncoder logEncoder, LogAppender logAppender,
                                                  List<AlertAppender> alertAppenders) {
        LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger rootLogger = ctx.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);

        rootLogger.detachAppender("LOGGING_SDK");

        SdkLogbackAppender appender = new SdkLogbackAppender(logEncoder, logAppender, alertAppenders);
        appender.setName("LOGGING_SDK");
        appender.setContext(ctx);
        appender.start();
        rootLogger.addAppender(appender);
        return appender;
    }
}