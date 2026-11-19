package com.tollfinder.service;

import com.tollfinder.client.RoutingClient;
import com.tollfinder.config.MatchProperties;
import com.tollfinder.exception.InvalidPincodeException;
import com.tollfinder.exception.SamePincodeException;
import com.tollfinder.model.Coordinate;
import com.tollfinder.model.RouteGeometry;
import com.tollfinder.model.TollPlaza;
import com.tollfinder.model.TollResponse;
import com.tollfinder.repository.TollPlazaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TollPlazaServiceTest {

    private static final Coordinate SOURCE = new Coordinate(12, 77.0);
    private static final Coordinate DESTINATION = new Coordinate(12, 78.0);

    @Mock PincodeLocator locator;
    @Mock RoutingClient routingClient;
    @Mock TollPlazaRepository repository;

    private TollPlazaService service;

    @BeforeEach
    void setUp() {
        MatchProperties props = new MatchProperties(300, 1000);
        service = new TollPlazaService(locator, routingClient, repository, new TollMatcher(props), props);
    }

    private void routeExists() {
        when(locator.locate("560064")).thenReturn(SOURCE);
        when(locator.locate("411045")).thenReturn(DESTINATION);
        when(routingClient.route(SOURCE, DESTINATION)).thenReturn(new RouteGeometry(
                List.of(SOURCE, new Coordinate(12, 77.5), DESTINATION), 108_800));
    }

    private void plazasInBox(TollPlaza... plazas) {
        when(repository.findByLatitudeBetweenAndLongitudeBetween(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of(plazas));
    }

    @Test
    void returnsRouteAndPlazasSortedByDistanceInKm() {
        routeExists();
        plazasInBox(new TollPlaza("Later", "KA", 12.0, 77.75), new TollPlaza("Earlier", "KA", 12.0005, 77.25));

        TollResponse response = service.findTollPlazas("560064", "411045");

        assertThat(response.route().sourcePincode()).isEqualTo("560064");
        assertThat(response.route().destinationPincode()).isEqualTo("411045");
        assertThat(response.route().distanceInKm()).isEqualTo(108.8);
        assertThat(response.tollPlazas()).extracting(TollResponse.TollPlazaInfo::name)
                .containsExactly("Earlier", "Later");
        assertThat(response.tollPlazas().get(0).distanceFromSource()).isEqualTo(27.19);
        assertThat(response.tollPlazas().get(0).latitude()).isEqualTo(12.0005);
    }

    @Test
    void noPlazasOnRouteIsNotAnError() {
        routeExists();
        plazasInBox();

        TollResponse response = service.findTollPlazas("560064", "411045");

        assertThat(response.tollPlazas()).isEmpty();
        assertThat(response.route().distanceInKm()).isEqualTo(108.8);
    }

    @Test
    void candidatesAwayFromTheLineAreFilteredOut() {
        routeExists();
        plazasInBox(new TollPlaza("Off route", "KA", 12.05, 77.5));

        assertThat(service.findTollPlazas("560064", "411045").tollPlazas()).isEmpty();
    }

    @Test
    void samePincodeFailsBeforeAnyExternalCall() {
        assertThatThrownBy(() -> service.findTollPlazas("560064", "560064")).isInstanceOf(SamePincodeException.class);

        verifyNoInteractions(locator, routingClient, repository);
    }

    @Test
    void unknownPincodeStopsBeforeRouting() {
        when(locator.locate("999999")).thenThrow(new InvalidPincodeException());

        assertThatThrownBy(() -> service.findTollPlazas("999999", "411045")).isInstanceOf(InvalidPincodeException.class);

        verifyNoInteractions(routingClient, repository);
    }
}
