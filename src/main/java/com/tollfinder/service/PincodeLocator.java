package com.tollfinder.service;

import com.tollfinder.client.GeocodingClient;
import com.tollfinder.model.Coordinate;
import org.springframework.stereotype.Service;

/** Turns a pincode into a coordinate. A separate bean so the lookup can be cached on its own. */
@Service
public class PincodeLocator {

    private final GeocodingClient geocodingClient;

    public PincodeLocator(GeocodingClient geocodingClient) {
        this.geocodingClient = geocodingClient;
    }

    public Coordinate locate(String pincode) {
        return geocodingClient.geocode(pincode);
    }
}
