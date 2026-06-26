package com.chobolevel.logging.appender;

import com.chobolevel.logging.core.LogLevel;
import com.chobolevel.logging.core.LogRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.DisposableBean;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SlackWebhookAppender implements AlertAppender, DisposableBean {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String webhookUrl;
    private final LogLevel minLevel;
    private final HttpClient httpClient;
    private ExecutorService executor;

    public SlackWebhookAppender(String webhookUrl, LogLevel minLevel) {
        this.webhookUrl = webhookUrl;
        this.minLevel = minLevel;
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public void start() {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            throw new IllegalStateException("[logging-sdk] logging-sdk.slack.webhook-url must be set when slack is enabled");
        }
        executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "logging-sdk-slack");
            t.setDaemon(true);
            return t;
        });
    }

    @Override
    public void stop() {
        if (executor != null) {
            executor.shutdown();
            try {
                executor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void destroy() {
        stop();
    }

    @Override
    public void send(LogRecord record) {
        if (record.getLevel().ordinal() < minLevel.ordinal()) {
            return;
        }
        String payload = buildPayload(record);
        executor.submit(() -> sendHttp(payload));
    }

    private void sendHttp(String payload) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            System.err.println("[logging-sdk] Slack webhook send failed: " + e.getMessage());
        }
    }

    private String buildPayload(LogRecord record) {
        Map<String, Object> attachment = new LinkedHashMap<>();
        attachment.put("color", colorOf(record.getLevel()));
        attachment.put("title", record.getLevel().name());
        attachment.put("text", record.getMessage());

        List<Map<String, Object>> fields = new ArrayList<>();
        fields.add(field("Logger", record.getLoggerName(), true));
        fields.add(field("Thread", record.getThreadName(), true));

        if (record.getMdc() != null && record.getMdc().containsKey("traceId")) {
            fields.add(field("TraceId", record.getMdc().get("traceId"), true));
        }
        if (record.getThrowableMessage() != null) {
            fields.add(field("Exception", record.getThrowableMessage(), false));
        }

        attachment.put("fields", fields);
        attachment.put("ts", record.getTimestamp().getEpochSecond());

        try {
            return MAPPER.writeValueAsString(Map.of("attachments", List.of(attachment)));
        } catch (Exception e) {
            return "{\"text\":\"[logging-sdk] payload encoding failed\"}";
        }
    }

    private Map<String, Object> field(String title, String value, boolean isShort) {
        return Map.of("title", title, "value", value, "short", isShort);
    }

    private String colorOf(LogLevel level) {
        return switch (level) {
            case ERROR -> "#FF0000";
            case WARN  -> "#FFA500";
            case INFO  -> "#36A64F";
            case DEBUG -> "#808080";
            case TRACE -> "#D3D3D3";
        };
    }
}