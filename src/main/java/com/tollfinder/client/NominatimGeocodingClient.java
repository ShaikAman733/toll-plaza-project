package com.tollfinder.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.tollfinder.config.NominatimProperties;
import com.tollfinder.exception.InvalidPincodeException;
import com.tollfinder.model.Coordinate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** OpenStreetMap geocoder: understands Indian postal codes directly and needs no API key. */
@Component
@ConditionalOnProperty(name = "geocoding.provider", havingValue = "nominatim", matchIfMissing = true)
public class NominatimGeocodingClient implements GeocodingClient {

    private final RestClient http;

    public NominatimGeocodingClient(RestClient.Builder builder, NominatimProperties props) {
        // Nominatim's usage policy requires an identifying User-Agent
        this.http = builder.baseUrl(props.baseUrl())
                .defaultHeader(HttpHeaders.USER_AGENT, "toll-plaza-finder/1.0")
                .build();
    }

    @Override
    public Coordinate geocode(String pincode) {
        JsonNode results = ExternalCalls.call("Geocoding service", () -> http.get()
                .uri("/search?postalcode={pin}&countrycodes=in&format=jsonv2&limit=1", pincode)
                .retrieve()
                .body(JsonNode.class));
        if (results == null || results.isEmpty()) {
            throw new InvalidPincodeException();
        }
        JsonNode best = results.get(0);
        return new Coordinate(best.get("lat").asDouble(), best.get("lon").asDouble());
    }
}
