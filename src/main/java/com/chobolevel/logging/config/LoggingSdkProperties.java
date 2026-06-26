package com.chobolevel.logging.config;

import com.chobolevel.logging.core.LogLevel;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "logging-sdk")
public class LoggingSdkProperties {

    private EncoderType encoder = EncoderType.PLAIN;
    private boolean async = false;
    private int asyncQueueSize = 1000;
    private ConsoleProperties console = new ConsoleProperties();
    private FileProperties file = new FileProperties();
    private SlackProperties slack = new SlackProperties();

    public enum EncoderType {
        JSON, PLAIN
    }

    @Data
    public static class ConsoleProperties {
        private boolean enabled = true;
    }

    @Data
    public static class FileProperties {
        private boolean enabled = false;
        private String directory = "logs";
        private String pattern = "app-%s.log";
    }

    @Data
    public static class SlackProperties {
        private boolean enabled = false;
        private String webhookUrl;
        private LogLevel minLevel = LogLevel.ERROR;
    }
}