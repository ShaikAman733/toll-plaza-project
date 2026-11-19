package com.tollfinder.service;

import com.tollfinder.config.MatchProperties;
import com.tollfinder.model.Coordinate;
import com.tollfinder.model.RouteGeometry;
import com.tollfinder.model.TollPlaza;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Pure logic, no I/O: decides which plazas lie on a route and where along it. */
@Component
public class TollMatcher {

    private final MatchProperties props;

    public TollMatcher(MatchProperties props) {
        this.props = props;
    }

    public List<TollMatch> match(RouteGeometry route, List<TollPlaza> candidates) {
        List<Coordinate> points = route.points();
        if (points.size() < 2) {
            return List.of();
        }
        double[] distanceAtPoint = cumulativeDistances(points);

        List<TollMatch> matches = new ArrayList<>();
        for (TollPlaza plaza : candidates) {
            TollMatch match = closestPointOnRoute(plaza, points, distanceAtPoint);
            if (match.offRouteMeters() <= props.toleranceMeters()) {
                matches.add(match);
            }
        }
        matches.sort(Comparator.comparingDouble(TollMatch::distanceFromSourceMeters));
        return mergeDuplicates(matches);
    }

    private double[] cumulativeDistances(List<Coordinate> points) {
        double[] cumulative = new double[points.size()];
        for (int i = 1; i < points.size(); i++) {
            cumulative[i] = cumulative[i - 1] + GeoMath.haversineMeters(points.get(i - 1), points.get(i));
        }
        return cumulative;
    }

    private TollMatch closestPointOnRoute(TollPlaza plaza, List<Coordinate> points, double[] distanceAtPoint) {
        Coordinate location = plaza.location();
        double bestOffRoute = Double.MAX_VALUE;
        double bestAlongRoute = 0;
        for (int i = 0; i < points.size() - 1; i++) {
            Coordinate a = points.get(i);
            Coordinate b = points.get(i + 1);
            if (clearlyTooFar(location, a, b)) {
                continue;
            }
            GeoMath.SegmentHit hit = GeoMath.nearestOnSegment(location, a, b);
            if (hit.distanceMeters() < bestOffRoute) {
                bestOffRoute = hit.distanceMeters();
                bestAlongRoute = distanceAtPoint[i] + hit.fractionAlong() * (distanceAtPoint[i + 1] - distanceAtPoint[i]);
            }
        }
        return new TollMatch(plaza, bestAlongRoute, bestOffRoute);
    }

    // Cheap rejection so a long route (thousands of segments) is not measured exactly for every plaza.
    private boolean clearlyTooFar(Coordinate p, Coordinate a, Coordinate b) {
        double latMargin = props.toleranceMeters() / GeoMath.METERS_PER_DEGREE_LAT;
        double lonMargin = latMargin / Math.cos(Math.toRadians(p.latitude()));
        return p.latitude() < Math.min(a.latitude(), b.latitude()) - latMargin
                || p.latitude() > Math.max(a.latitude(), b.latitude()) + latMargin
                || p.longitude() < Math.min(a.longitude(), b.longitude()) - lonMargin
                || p.longitude() > Math.max(a.longitude(), b.longitude()) + lonMargin;
    }

    // The data often lists both directions of one highway plaza. Same name and close together
    // along the route means the same plaza, so keep only the one met first.
    private List<TollMatch> mergeDuplicates(List<TollMatch> sortedByDistance) {
        List<TollMatch> merged = new ArrayList<>();
        for (TollMatch candidate : sortedByDistance) {
            if (merged.stream().noneMatch(kept -> isSamePlaza(kept, candidate))) {
                merged.add(candidate);
            }
        }
        return merged;
    }

    private boolean isSamePlaza(TollMatch kept, TollMatch other) {
        boolean sameName = kept.plaza().getName().trim().equalsIgnoreCase(other.plaza().getName().trim());
        boolean close = other.distanceFromSourceMeters() - kept.distanceFromSourceMeters() <= props.mergeDistanceMeters();
        return sameName && close;
    }
}
