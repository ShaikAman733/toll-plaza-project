package com.tollfinder.service;

import com.tollfinder.model.TollPlaza;

/** A plaza found on the route, with its position along the route and how far it sits off the line. */
public record TollMatch(TollPlaza plaza, double distanceFromSourceMeters, double offRouteMeters) {
}
