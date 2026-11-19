package com.tollfinder.client;

import com.tollfinder.config.OrsProperties;
import com.tollfinder.exception.ExternalServiceException;
import com.tollfinder.exception.RouteNotFoundException;
import com.tollfinder.model.Coordinate;
import com.tollfinder.model.RouteGeometry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class OrsRoutingClientTest {

    private static final String URL = "https://ors.test/openrouteservice/v2/directions/driving-car/geojson";
    private static final Coordinate FROM = new Coordinate(12.99, 77.5);
    private static final Coordinate TO = new Coordinate(18.56, 73.78);

    private MockRestServiceServer server;
    private OrsRoutingClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new OrsRoutingClient(builder,
                new OrsProperties("https://ors.test/openrouteservice", "unused", "secret-key", 5));
    }

    @Test
    void parsesGeometryAndDistance() {
        server.expect(requestTo(URL))
                .andExpect(header("Authorization", "secret-key"))
                // ORS expects longitude first
                .andExpect(jsonPath("$.coordinates[0][0]").value(77.5))
                .andExpect(jsonPath("$.coordinates[0][1]").value(12.99))
                .andRespond(withSuccess("""
                        {"features":[{"geometry":{"coordinates":[[77.5,12.99],[75.0,15.0],[73.78,18.56]]},
                                      "properties":{"summary":{"distance":842000.5}}}]}""", APPLICATION_JSON));

        RouteGeometry route = client.route(FROM, TO);

        assertThat(route.distanceMeters()).isEqualTo(842000.5);
        assertThat(route.points()).containsExactly(
                new Coordinate(12.99, 77.5), new Coordinate(15.0, 75.0), new Coordinate(18.56, 73.78));
    }

    @Test
    void unroutablePointMeansRouteNotFound() {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.route(FROM, TO)).isInstanceOf(RouteNotFoundException.class);
    }

    @Test
    void rateLimitMapsTo429() {
        server.expect(requestTo(URL)).andRespond(withTooManyRequests());

        assertThatThrownBy(() -> client.route(FROM, TO))
                .isInstanceOfSatisfying(ExternalServiceException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
    }

    @Test
    void serverErrorMapsTo502WithoutLeakingTheKey() {
        server.expect(requestTo(URL)).andRespond(withServerError());

        assertThatThrownBy(() -> client.route(FROM, TO))
                .isInstanceOfSatisfying(ExternalServiceException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
                    assertThat(e.getMessage()).doesNotContain("secret-key");
                });
    }

    @Test
    void timeoutMapsTo504() {
        server.expect(requestTo(URL)).andRespond(withException(new SocketTimeoutException("read timed out")));

        assertThatThrownBy(() -> client.route(FROM, TO))
                .isInstanceOfSatisfying(ExternalServiceException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT));
    }

    @Test
    void responseWithoutRouteIsABadGateway() {
        server.expect(requestTo(URL)).andRespond(withSuccess("{\"features\":[]}", APPLICATION_JSON));

        assertThatThrownBy(() -> client.route(FROM, TO))
                .isInstanceOfSatisfying(ExternalServiceException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY));
    }
}
