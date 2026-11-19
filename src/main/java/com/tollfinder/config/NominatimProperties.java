package com.tollfinder.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("nominatim")
public record NominatimProperties(String baseUrl) {
}
