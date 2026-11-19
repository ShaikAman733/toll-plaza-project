package com.tollfinder.service;

import com.tollfinder.model.Coordinate;

import java.util.List;

/** Rectangle around a route, used to ask the database only for plazas that could possibly be on it. */
public record BoundingBox(double minLat, double maxLat, double minLon, double maxLon) {

    public static BoundingBox around(List<Coordinate> points, double marginMeters) {
        double minLat = Double.MAX_VALUE, maxLat = -Double.MAX_VALUE;
        double minLon = Double.MAX_VALUE, maxLon = -Double.MAX_VALUE;
        for (Coordinate p : points) {
            minLat = Math.min(minLat, p.latitude());
            maxLat = Math.max(maxLat, p.latitude());
            minLon = Math.min(minLon, p.longitude());
            maxLon = Math.max(maxLon, p.longitude());
        }
        double latMargin = marginMeters / GeoMath.METERS_PER_DEGREE_LAT;
        // a degree of longitude is shorter away from the equator, so widen by the worst (northernmost) case
        double lonMargin = latMargin / Math.cos(Math.toRadians(Math.max(Math.abs(minLat), Math.abs(maxLat))));
        return new BoundingBox(minLat - latMargin, maxLat + latMargin, minLon - lonMargin, maxLon + lonMargin);
    }
}
