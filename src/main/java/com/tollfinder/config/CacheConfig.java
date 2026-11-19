package com.tollfinder.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/** Caffeine caches ("pincodes", "tollRoutes") are defined in application.yml. */
@Configuration
@EnableCaching
public class CacheConfig {
}
