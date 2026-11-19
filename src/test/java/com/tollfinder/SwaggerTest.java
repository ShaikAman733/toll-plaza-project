package com.tollfinder;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Guards against the global error handler breaking the OpenAPI document. */
@SpringBootTest(properties = "toll.load-on-startup=false")
@AutoConfigureMockMvc
class SwaggerTest {

    @Autowired MockMvc mvc;

    @Test
    void openApiDocumentDescribesTheEndpoint() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/toll-plazas'].post.summary").exists())
                .andExpect(jsonPath("$.paths['/api/v1/toll-plazas'].post.responses['400']").exists());
    }

    @Test
    void swaggerUiIsServed() throws Exception {
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }
}
