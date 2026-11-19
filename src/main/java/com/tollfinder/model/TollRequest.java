package com.tollfinder.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record TollRequest(
        @NotNull(message = TollRequest.INVALID) @Pattern(regexp = TollRequest.PINCODE, message = TollRequest.INVALID)
        String sourcePincode,
        @NotNull(message = TollRequest.INVALID) @Pattern(regexp = TollRequest.PINCODE, message = TollRequest.INVALID)
        String destinationPincode) {

    public static final String INVALID = "Invalid source or destination pincode";
    // six digits, and Indian pincodes never start with 0
    public static final String PINCODE = "[1-9][0-9]{5}";
}
