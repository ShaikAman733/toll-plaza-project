package com.tollfinder.client;

import com.tollfinder.model.Coordinate;
import com.tollfinder.model.RouteGeometry;

public interface RoutingClient {

    RouteGeometry route(Coordinate from, Coordinate to);
}
