package com.tollfinder.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI openApi() {
        return new OpenAPI().info(new Info()
                .title("Toll Plaza Finder API")
                .version("1.0.0")
                .description("Returns the toll plazas on the driving route between two Indian pincodes."));
    }
}
