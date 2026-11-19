package com.tollfinder.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Base URLs are kept without a trailing slash. */
@ConfigurationProperties("ors")
public record OrsProperties(String directionsBaseUrl, String geocodingBaseUrl, String apiKey, int timeoutSeconds) {
}
