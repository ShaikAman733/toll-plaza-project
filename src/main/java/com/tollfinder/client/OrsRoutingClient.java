package com.tollfinder.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.tollfinder.config.OrsProperties;
import com.tollfinder.exception.ExternalServiceException;
import com.tollfinder.exception.RouteNotFoundException;
import com.tollfinder.model.Coordinate;
import com.tollfinder.model.RouteGeometry;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class OrsRoutingClient implements RoutingClient {

    private static final String PROVIDER = "Routing service";

    private final RestClient http;

    public OrsRoutingClient(RestClient.Builder builder, OrsProperties props) {
        this.http = builder.baseUrl(props.directionsBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, props.apiKey())
                .build();
    }

    @Override
    public RouteGeometry route(Coordinate from, Coordinate to) {
        // ORS wants [longitude, latitude]; radiuses -1 means "snap to the nearest road, however far":
        // a pincode centre is often more than the default 350 m from a road
        Map<String, Object> body = Map.of(
                "coordinates", List.of(
                        List.of(from.longitude(), from.latitude()),
                        List.of(to.longitude(), to.latitude())),
                "radiuses", List.of(-1, -1));
        return parse(ExternalCalls.call(PROVIDER, () -> post(body)));
    }

    private JsonNode post(Map<String, Object> body) {
        try {
            return http.post()
                    .uri("/v2/directions/driving-car/geojson")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.parseMediaType("application/geo+json"), MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (HttpClientErrorException.NotFound | HttpClientErrorException.BadRequest e) {
            // ORS answers 404 (code 2010) when a point cannot be snapped to a road
            throw new RouteNotFoundException();
        }
    }

    private RouteGeometry parse(JsonNode response) {
        JsonNode feature = response == null ? null : response.path("features").path(0);
        if (feature == null || feature.isMissingNode()) {
            throw new ExternalServiceException(HttpStatus.BAD_GATEWAY, PROVIDER + " returned an unexpected response");
        }
        List<Coordinate> points = new ArrayList<>();
        for (JsonNode point : feature.path("geometry").path("coordinates")) {
            points.add(new Coordinate(point.get(1).asDouble(), point.get(0).asDouble()));
        }
        double distance = feature.path("properties").path("summary").path("distance").asDouble();
        return new RouteGeometry(points, distance);
    }
}
