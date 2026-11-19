package com.tollfinder.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.tollfinder.config.OrsProperties;
import com.tollfinder.exception.InvalidPincodeException;
import com.tollfinder.model.Coordinate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** ORS (Pelias) geocoder. Postal-code coverage for India is unverified, so it is opt-in: geocoding.provider=ors */
@Component
@ConditionalOnProperty(name = "geocoding.provider", havingValue = "ors")
public class OrsGeocodingClient implements GeocodingClient {

    private final RestClient http;

    public OrsGeocodingClient(RestClient.Builder builder, OrsProperties props) {
        this.http = builder.baseUrl(props.geocodingBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, props.apiKey())
                .build();
    }

    @Override
    public Coordinate geocode(String pincode) {
        JsonNode response = ExternalCalls.call("Geocoding service", () -> http.get()
                .uri("/search?text={pin}&boundary.country=IN&layers=postalcode&size=1", pincode)
                .retrieve()
                .body(JsonNode.class));
        JsonNode coordinates = response == null ? null : response.path("features").path(0).path("geometry").path("coordinates");
        if (coordinates == null || coordinates.isMissingNode() || coordinates.size() < 2) {
            throw new InvalidPincodeException();
        }
        return new Coordinate(coordinates.get(1).asDouble(), coordinates.get(0).asDouble());
    }
}
