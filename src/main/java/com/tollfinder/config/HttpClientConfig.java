package com.tollfinder.config;

import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class HttpClientConfig {

    /** Every RestClient gets timeouts, so a slow provider cannot hang our request threads. */
    @Bean
    RestClientCustomizer timeouts(OrsProperties ors) {
        Duration timeout = Duration.ofSeconds(ors.timeoutSeconds());
        return builder -> {
            JdkClientHttpRequestFactory factory =
                    new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(timeout).build());
            factory.setReadTimeout(timeout);
            builder.requestFactory(factory);
        };
    }
}
