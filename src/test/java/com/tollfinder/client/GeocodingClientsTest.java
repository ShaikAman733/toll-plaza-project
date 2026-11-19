package com.tollfinder.client;

import com.tollfinder.config.NominatimProperties;
import com.tollfinder.config.OrsProperties;
import com.tollfinder.exception.ExternalServiceException;
import com.tollfinder.exception.InvalidPincodeException;
import com.tollfinder.model.Coordinate;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class GeocodingClientsTest {

    @Test
    void nominatimReturnsCoordinateOfFirstResult() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        NominatimGeocodingClient client = new NominatimGeocodingClient(builder, new NominatimProperties("https://nominatim.test"));
        server.expect(requestTo("https://nominatim.test/search?postalcode=560064&countrycodes=in&format=jsonv2&limit=1"))
                .andExpect(header("User-Agent", "toll-plaza-finder/1.0"))
                .andRespond(withSuccess("[{\"lat\":\"13.1401693\",\"lon\":\"77.5722511\"}]", APPLICATION_JSON));

        assertThat(client.geocode("560064")).isEqualTo(new Coordinate(13.1401693, 77.5722511));
    }

    @Test
    void nominatimEmptyResultMeansInvalidPincode() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        NominatimGeocodingClient client = new NominatimGeocodingClient(builder, new NominatimProperties("https://nominatim.test"));
        server.expect(requestTo("https://nominatim.test/search?postalcode=999999&countrycodes=in&format=jsonv2&limit=1"))
                .andRespond(withSuccess("[]", APPLICATION_JSON));

        assertThatThrownBy(() -> client.geocode("999999")).isInstanceOf(InvalidPincodeException.class);
    }

    @Test
    void nominatimDownMapsTo502() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        NominatimGeocodingClient client = new NominatimGeocodingClient(builder, new NominatimProperties("https://nominatim.test"));
        server.expect(requestTo("https://nominatim.test/search?postalcode=560064&countrycodes=in&format=jsonv2&limit=1"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.geocode("560064"))
                .isInstanceOfSatisfying(ExternalServiceException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY));
    }

    private static final OrsProperties ORS = new OrsProperties("unused", "https://pelias.test/v1", "secret-key", 5);
    private static final String ORS_URL = "https://pelias.test/v1/search?text=411045&boundary.country=IN&layers=postalcode&size=1";

    @Test
    void orsReturnsCoordinateFromGeoJsonPoint() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OrsGeocodingClient client = new OrsGeocodingClient(builder, ORS);
        server.expect(requestTo(ORS_URL))
                .andExpect(header("Authorization", "secret-key"))
                .andRespond(withSuccess("{\"features\":[{\"geometry\":{\"coordinates\":[73.78,18.56]}}]}", APPLICATION_JSON));

        assertThat(client.geocode("411045")).isEqualTo(new Coordinate(18.56, 73.78));
    }

    @Test
    void orsNoFeaturesMeansInvalidPincode() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OrsGeocodingClient client = new OrsGeocodingClient(builder, ORS);
        server.expect(requestTo(ORS_URL)).andRespond(withSuccess("{\"features\":[]}", APPLICATION_JSON));

        assertThatThrownBy(() -> client.geocode("411045")).isInstanceOf(InvalidPincodeException.class);
    }

    @Test
    void orsRateLimitMapsTo429() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OrsGeocodingClient client = new OrsGeocodingClient(builder, ORS);
        server.expect(requestTo(ORS_URL)).andRespond(withTooManyRequests());

        assertThatThrownBy(() -> client.geocode("411045"))
                .isInstanceOfSatisfying(ExternalServiceException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
    }
}
