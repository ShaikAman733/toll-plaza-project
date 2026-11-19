package com.tollfinder.client;

import com.tollfinder.model.Coordinate;

public interface GeocodingClient {

    /** @throws com.tollfinder.exception.InvalidPincodeException if the pincode cannot be located */
    Coordinate geocode(String pincode);
}
