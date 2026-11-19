package com.tollfinder;

import com.tollfinder.client.GeocodingClient;
import com.tollfinder.client.RoutingClient;
import com.tollfinder.exception.InvalidPincodeException;
import com.tollfinder.model.Coordinate;
import com.tollfinder.model.RouteGeometry;
import com.tollfinder.service.TollPlazaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/** Full Spring context (so the cache proxies are active) but with both external clients mocked. */
@SpringBootTest(properties = "toll.load-on-startup=false")
class CachingTest {

    private static final Coordinate BENGALURU = new Coordinate(13.14, 77.57);
    private static final Coordinate PUNE = new Coordinate(18.56, 73.78);
    private static final Coordinate DELHI = new Coordinate(28.63, 77.22);

    @Autowired TollPlazaService service;
    @Autowired CacheManager cacheManager;
    @MockitoBean GeocodingClient geocodingClient;
    @MockitoBean RoutingClient routingClient;

    @BeforeEach
    void setUp() {
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
        when(geocodingClient.geocode("560064")).thenReturn(BENGALURU);
        when(geocodingClient.geocode("411045")).thenReturn(PUNE);
        when(geocodingClient.geocode("110001")).thenReturn(DELHI);
        when(routingClient.route(any(), any())).thenReturn(new RouteGeometry(List.of(BENGALURU, PUNE), 840_000));
    }

    @Test
    void secondIdenticalCallDoesNotTouchTheClients() {
        service.findTollPlazas("560064", "411045");
        service.findTollPlazas("560064", "411045");

        verify(routingClient, times(1)).route(any(), any());
        verify(geocodingClient, times(1)).geocode("560064");
        verify(geocodingClient, times(1)).geocode("411045");
    }

    @Test
    void reversedDirectionIsADifferentRouteButReusesCachedPincodeLookups() {
        service.findTollPlazas("560064", "411045");
        service.findTollPlazas("411045", "560064");

        verify(routingClient, times(2)).route(any(), any());
        verify(geocodingClient, times(1)).geocode("560064");
        verify(geocodingClient, times(1)).geocode("411045");
    }

    @Test
    void newDestinationOnlyGeocodesTheNewPincode() {
        service.findTollPlazas("560064", "411045");
        service.findTollPlazas("560064", "110001");

        verify(geocodingClient, times(1)).geocode("560064");
        verify(geocodingClient, times(1)).geocode("110001");
        verify(routingClient, times(2)).route(any(), any());
    }

    @Test
    void failedLookupsAreNotCached() {
        when(geocodingClient.geocode("999999")).thenThrow(new InvalidPincodeException());

        assertThatThrownBy(() -> service.findTollPlazas("999999", "411045")).isInstanceOf(InvalidPincodeException.class);
        assertThatThrownBy(() -> service.findTollPlazas("999999", "411045")).isInstanceOf(InvalidPincodeException.class);

        verify(geocodingClient, times(2)).geocode("999999");
    }
}
