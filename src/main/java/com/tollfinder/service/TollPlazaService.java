package com.tollfinder.service;

import com.tollfinder.client.RoutingClient;
import com.tollfinder.config.MatchProperties;
import com.tollfinder.exception.SamePincodeException;
import com.tollfinder.model.Coordinate;
import com.tollfinder.model.RouteGeometry;
import com.tollfinder.model.TollPlaza;
import com.tollfinder.model.TollResponse;
import com.tollfinder.model.TollResponse.RouteInfo;
import com.tollfinder.model.TollResponse.TollPlazaInfo;
import com.tollfinder.repository.TollPlazaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TollPlazaService {

    private final PincodeLocator locator;
    private final RoutingClient routingClient;
    private final TollPlazaRepository repository;
    private final TollMatcher matcher;
    private final MatchProperties matchProperties;

    public TollPlazaService(PincodeLocator locator, RoutingClient routingClient, TollPlazaRepository repository,
                            TollMatcher matcher, MatchProperties matchProperties) {
        this.locator = locator;
        this.routingClient = routingClient;
        this.repository = repository;
        this.matcher = matcher;
        this.matchProperties = matchProperties;
    }

    public TollResponse findTollPlazas(String sourcePincode, String destinationPincode) {
        if (sourcePincode.equals(destinationPincode)) {
            throw new SamePincodeException();
        }
        Coordinate source = locator.locate(sourcePincode);
        Coordinate destination = locator.locate(destinationPincode);
        RouteGeometry route = routingClient.route(source, destination);

        BoundingBox box = BoundingBox.around(route.points(), matchProperties.toleranceMeters());
        List<TollPlaza> candidates = repository.findByLatitudeBetweenAndLongitudeBetween(
                box.minLat(), box.maxLat(), box.minLon(), box.maxLon());
        List<TollPlazaInfo> plazas = matcher.match(route, candidates).stream()
                .map(m -> new TollPlazaInfo(m.plaza().getName(), m.plaza().getLatitude(), m.plaza().getLongitude(),
                        toKm(m.distanceFromSourceMeters())))
                .toList();

        return new TollResponse(new RouteInfo(sourcePincode, destinationPincode, toKm(route.distanceMeters())), plazas);
    }

    private double toKm(double meters) {
        return Math.round(meters / 10.0) / 100.0;
    }
}
