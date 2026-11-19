package com.tollfinder.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.GET;

class KeepAliveTest {

    @Test
    void pingsTheHealthEndpointOfThePublicUrl() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://toll.onrender.com/health")).andExpect(method(GET))
                .andRespond(withSuccess("{\"status\":\"UP\"}", org.springframework.http.MediaType.APPLICATION_JSON));

        new KeepAlive(builder, "https://toll.onrender.com").ping();

        server.verify();
    }

    @Test
    void doesNothingWhenNoPublicUrlIsConfigured() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        new KeepAlive(builder, "").ping();

        server.verify(); // no request was expected, none was made
    }

    @Test
    void aFailedPingIsSwallowed() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://toll.onrender.com/health")).andRespond(withServerError());

        assertThatCode(() -> new KeepAlive(builder, "https://toll.onrender.com").ping()).doesNotThrowAnyException();
    }
}
