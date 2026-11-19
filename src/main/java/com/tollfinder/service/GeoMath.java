package com.tollfinder.service;

import com.tollfinder.model.Coordinate;

final class GeoMath {

    static final double METERS_PER_DEGREE_LAT = 111_194.9; // earth radius 6371 km * pi / 180

    private GeoMath() {
    }

    static double haversineMeters(Coordinate a, Coordinate b) {
        double dLat = Math.toRadians(b.latitude() - a.latitude());
        double dLon = Math.toRadians(b.longitude() - a.longitude());
        double h = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(a.latitude())) * Math.cos(Math.toRadians(b.latitude())) * Math.pow(Math.sin(dLon / 2), 2);
        return 2 * 6_371_000 * Math.asin(Math.sqrt(h));
    }

    /** Where the closest point on segment a-b lies, and how far the point is from it. */
    record SegmentHit(double distanceMeters, double fractionAlong) {
    }

    /**
     * Treats the area around the point as flat (metres east/north of the point). Over a 300 m tolerance
     * the error from ignoring the earth's curvature is far below a metre.
     */
    static SegmentHit nearestOnSegment(Coordinate point, Coordinate a, Coordinate b) {
        double lonScale = METERS_PER_DEGREE_LAT * Math.cos(Math.toRadians(point.latitude()));
        double ax = (a.longitude() - point.longitude()) * lonScale;
        double ay = (a.latitude() - point.latitude()) * METERS_PER_DEGREE_LAT;
        double dx = (b.longitude() - a.longitude()) * lonScale;
        double dy = (b.latitude() - a.latitude()) * METERS_PER_DEGREE_LAT;

        double lengthSquared = dx * dx + dy * dy;
        double t = lengthSquared == 0 ? 0 : Math.max(0, Math.min(1, -(ax * dx + ay * dy) / lengthSquared));
        return new SegmentHit(Math.hypot(ax + t * dx, ay + t * dy), t);
    }
}
