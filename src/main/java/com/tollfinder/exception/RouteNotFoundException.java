package com.tollfinder.exception;

public class RouteNotFoundException extends RuntimeException {

    public RouteNotFoundException() {
        super("No drivable route found between the given pincodes");
    }
}
