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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TollPlazaService {

    private static final Logger log = LoggerFactory.getLogger(TollPlazaService.class);

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

    // one entry per direction: A to B and B to A are different routes
    @Cacheable(cacheNames = "tollRoutes", key = "#sourcePincode + '-' + #destinationPincode")
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
        List<TollMatch> matches = matcher.match(route, candidates);
        List<Coordinate> points = route.points();
        log.info("Route {}->{}: {} points, first {}, last {}, box {}, {} candidates, {} matched",
                sourcePincode, destinationPincode, points.size(), points.get(0), points.get(points.size() - 1),
                box, candidates.size(), matches.size());
        List<TollPlazaInfo> plazas = matches.stream()
                .map(m -> new TollPlazaInfo(m.plaza().getName(), m.plaza().getLatitude(), m.plaza().getLongitude(),
                        toKm(m.distanceFromSourceMeters())))
                .toList();

        return new TollResponse(new RouteInfo(sourcePincode, destinationPincode, toKm(route.distanceMeters())), plazas);
    }

    private double toKm(double meters) {
        return Math.round(meters / 10.0) / 100.0;
    }
}
