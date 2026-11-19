package com.tollfinder;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "toll.load-on-startup=false")
@AutoConfigureMockMvc
class HomeAndHealthTest {

    @Autowired MockMvc mvc;

    @Test
    void rootRedirectsToSwaggerUi() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/swagger-ui/index.html"));
    }

    @Test
    void healthReportsUp() throws Exception {
        mvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void healthEndpointIsNotListedInTheApiDocs() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.paths['/health']").doesNotExist());
    }
}
