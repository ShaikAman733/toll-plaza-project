package com.tollfinder.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("toll.match")
public record MatchProperties(double toleranceMeters, double mergeDistanceMeters) {
}
