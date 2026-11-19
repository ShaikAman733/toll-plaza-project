package com.tollfinder.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestClient;

/**
 * Render's free tier sleeps after 15 minutes without incoming requests. Calling our own public
 * /health every 10 minutes counts as incoming traffic. Only active when RENDER_EXTERNAL_URL is
 * set (Render sets it automatically), so it does nothing locally or in tests.
 */
@Configuration
@EnableScheduling
public class KeepAlive {

    private static final Logger log = LoggerFactory.getLogger(KeepAlive.class);

    private final RestClient http;
    private final String publicUrl;

    public KeepAlive(RestClient.Builder builder, @Value("${keep-alive.url:}") String publicUrl) {
        this.http = builder.build();
        this.publicUrl = publicUrl;
    }

    @Scheduled(initialDelay = 600_000, fixedDelay = 600_000)
    void ping() {
        if (publicUrl.isBlank()) {
            return;
        }
        try {
            http.get().uri(publicUrl + "/health").retrieve().toBodilessEntity();
        } catch (RuntimeException e) {
            log.warn("Keep-alive ping failed: {}", e.getMessage());
        }
    }
}
