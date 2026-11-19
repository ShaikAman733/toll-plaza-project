package com.tollfinder.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record TollRequest(
        @Schema(description = "6-digit pincode of the starting point", example = "560064")
        @NotNull(message = TollRequest.INVALID) @Pattern(regexp = TollRequest.PINCODE, message = TollRequest.INVALID)
        String sourcePincode,
        @Schema(description = "6-digit pincode of the destination", example = "411045")
        @NotNull(message = TollRequest.INVALID) @Pattern(regexp = TollRequest.PINCODE, message = TollRequest.INVALID)
        String destinationPincode) {

    public static final String INVALID = "Invalid source or destination pincode";
    // six digits, and Indian pincodes never start with 0
    public static final String PINCODE = "[1-9][0-9]{5}";
}
