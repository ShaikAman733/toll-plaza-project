package com.tollfinder.model;

import java.util.List;

public record TollResponse(RouteInfo route, List<TollPlazaInfo> tollPlazas) {

    public record RouteInfo(String sourcePincode, String destinationPincode, double distanceInKm) {
    }

    public record TollPlazaInfo(String name, double latitude, double longitude, double distanceFromSource) {
    }
}
