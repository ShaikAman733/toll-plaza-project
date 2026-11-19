package com.tollfinder.service;

import com.tollfinder.config.MatchProperties;
import com.tollfinder.model.Coordinate;
import com.tollfinder.model.RouteGeometry;
import com.tollfinder.model.TollPlaza;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

class TollMatcherTest {

    // 1 degree of latitude is about 111.2 km; 0.001 degrees is about 111 m
    private static final double DEG_LAT_M = 111_195;

    private final TollMatcher matcher = new TollMatcher(new MatchProperties(300, 1000));

    /** Straight route going east along latitude 12, from longitude 77.0 to 78.0 (about 108.8 km). */
    private final RouteGeometry eastRoute = new RouteGeometry(
            List.of(new Coordinate(12, 77.0), new Coordinate(12, 77.5), new Coordinate(12, 78.0)), 108_800);

    private TollPlaza plaza(String name, double lat, double lon) {
        return new TollPlaza(name, "Karnataka", lat, lon);
    }

    @Test
    void plazaNearTheRouteIsMatchedWithAlongRouteDistance() {
        List<TollMatch> matches = matcher.match(eastRoute, List.of(plaza("Near", 12.001, 77.25)));

        assertThat(matches).hasSize(1);
        // a quarter of a degree of longitude at latitude 12 is about 27.2 km
        assertThat(matches.get(0).distanceFromSourceMeters()).isCloseTo(27_192, offset(50.0));
        assertThat(matches.get(0).offRouteMeters()).isCloseTo(111, offset(5.0));
    }

    @Test
    void plazaBeyondToleranceIsIgnored() {
        // 0.01 degrees north is about 1.1 km off the route
        assertThat(matcher.match(eastRoute, List.of(plaza("Far", 12.01, 77.25)))).isEmpty();
    }

    @Test
    void plazaJustInsideAndJustOutsideTheToleranceBoundary() {
        double insideLat = 12 + 250 / DEG_LAT_M;
        double outsideLat = 12 + 350 / DEG_LAT_M;

        assertThat(matcher.match(eastRoute, List.of(plaza("In", insideLat, 77.25)))).hasSize(1);
        assertThat(matcher.match(eastRoute, List.of(plaza("Out", outsideLat, 77.25)))).isEmpty();
    }

    @Test
    void resultsAreSortedBySourceDistanceNotInputOrder() {
        List<TollMatch> matches = matcher.match(eastRoute, List.of(
                plaza("Third", 12.0, 77.9), plaza("First", 12.0, 77.1), plaza("Second", 12.0, 77.5)));

        assertThat(matches).extracting(m -> m.plaza().getName()).containsExactly("First", "Second", "Third");
    }

    @Test
    void distanceFollowsTheRouteAroundACornerNotTheStraightLine() {
        // east 0.5 degrees, then north 0.5 degrees
        RouteGeometry lRoute = new RouteGeometry(
                List.of(new Coordinate(12, 77.0), new Coordinate(12, 77.5), new Coordinate(12.5, 77.5)), 110_000);

        List<TollMatch> matches = matcher.match(lRoute, List.of(plaza("Corner", 12.5, 77.5)));

        double eastLeg = 0.5 * DEG_LAT_M * Math.cos(Math.toRadians(12));
        double northLeg = 0.5 * DEG_LAT_M;
        double straightLine = Math.hypot(eastLeg, northLeg);
        assertThat(matches.get(0).distanceFromSourceMeters()).isCloseTo(eastLeg + northLeg, offset(300.0));
        assertThat(matches.get(0).distanceFromSourceMeters()).isGreaterThan(straightLine + 5_000);
    }

    @Test
    void plazaJustBeforeTheStartOfTheRouteIsClampedToTheStart() {
        // 200 m west of the first point
        List<TollMatch> matches = matcher.match(eastRoute, List.of(plaza("Start", 12.0, 77.0 - 0.0018)));

        assertThat(matches).hasSize(1);
        assertThat(matches.get(0).distanceFromSourceMeters()).isZero();
    }

    @Test
    void sameNamePlazasCloseTogetherAreMergedIntoOne() {
        // the two carriageways of one highway: about 400 m apart along the route
        List<TollMatch> matches = matcher.match(eastRoute, List.of(
                plaza("Hosur Toll Plaza", 12.0005, 77.2500), plaza("Hosur Toll Plaza", 12.0005, 77.2537)));

        assertThat(matches).hasSize(1);
    }

    @Test
    void differentNamesCloseTogetherAreBothKept() {
        List<TollMatch> matches = matcher.match(eastRoute, List.of(
                plaza("Hosur Toll Plaza", 12.0005, 77.2500), plaza("Attibele Toll Plaza", 12.0005, 77.2537)));

        assertThat(matches).hasSize(2);
    }

    @Test
    void sameNameFarApartIsNotMerged() {
        List<TollMatch> matches = matcher.match(eastRoute, List.of(
                plaza("Common Name", 12.0, 77.2), plaza("Common Name", 12.0, 77.7)));

        assertThat(matches).hasSize(2);
    }

    @Test
    void sameNameComparisonIgnoresCaseAndSpaces() {
        List<TollMatch> matches = matcher.match(eastRoute, List.of(
                plaza("Hosur Toll Plaza", 12.0, 77.2500), plaza(" HOSUR toll plaza ", 12.0, 77.2537)));

        assertThat(matches).hasSize(1);
    }

    @Test
    void noCandidatesOrEmptyRouteGivesEmptyList() {
        assertThat(matcher.match(eastRoute, List.of())).isEmpty();
        assertThat(matcher.match(new RouteGeometry(List.of(new Coordinate(12, 77)), 0),
                List.of(plaza("X", 12, 77)))).isEmpty();
    }
}
