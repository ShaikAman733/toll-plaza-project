package com.tollfinder.service;

import com.tollfinder.client.GeocodingClient;
import com.tollfinder.model.Coordinate;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/** Turns a pincode into a coordinate. A separate bean so the lookup can be cached on its own. */
@Service
public class PincodeLocator {

    private final GeocodingClient geocodingClient;

    public PincodeLocator(GeocodingClient geocodingClient) {
        this.geocodingClient = geocodingClient;
    }

    // failed lookups throw, and Spring never caches exceptions
    @Cacheable("pincodes")
    public Coordinate locate(String pincode) {
        return geocodingClient.geocode(pincode);
    }
}
