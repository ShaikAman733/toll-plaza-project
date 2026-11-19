package com.tollfinder.model;

import java.util.List;

/** A driving route: the line as ordered points plus the total length reported by the provider. */
public record RouteGeometry(List<Coordinate> points, double distanceMeters) {
}
