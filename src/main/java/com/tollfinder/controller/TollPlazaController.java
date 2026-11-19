package com.tollfinder.controller;

import com.tollfinder.model.ErrorResponse;
import com.tollfinder.model.TollRequest;
import com.tollfinder.model.TollResponse;
import com.tollfinder.service.TollPlazaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/toll-plazas")
@Tag(name = "Toll plazas")
public class TollPlazaController {

    private final TollPlazaService service;

    public TollPlazaController(TollPlazaService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Find toll plazas between two pincodes",
            description = "Looks up the driving route between the two pincodes and returns the toll plazas on it, "
                    + "ordered by distance along the route from the source. Distances are in km. "
                    + "A route with no plazas is a normal 200 response with an empty list.")
    @ApiResponse(responseCode = "200", description = "Route found (tollPlazas may be empty)",
            content = @Content(schema = @Schema(implementation = TollResponse.class), examples = {
                    @ExampleObject(name = "Plazas on the route", value = """
                            {
                              "route": {"sourcePincode": "560064", "destinationPincode": "411045", "distanceInKm": 842.31},
                              "tollPlazas": [
                                {"name": "Kaggalipura Toll Plaza", "latitude": 12.7425, "longitude": 77.4528, "distanceFromSource": 31.52},
                                {"name": "Dharwad Toll Plaza", "latitude": 15.4721, "longitude": 74.9945, "distanceFromSource": 427.08}
                              ]
                            }"""),
                    @ExampleObject(name = "No plazas on the route", value = """
                            {
                              "route": {"sourcePincode": "110001", "destinationPincode": "110002", "distanceInKm": 3.4},
                              "tollPlazas": []
                            }""")}))
    @ApiResponse(responseCode = "400", description = "Invalid pincode (not 6 digits, or not found) or same source and destination",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = {
                    @ExampleObject(name = "Invalid pincode", value = "{\"error\": \"Invalid source or destination pincode\"}"),
                    @ExampleObject(name = "Same pincodes", value = "{\"error\": \"Source and destination pincodes cannot be the same\"}")}))
    @ApiResponse(responseCode = "422", description = "No drivable route between the pincodes",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"error\": \"No drivable route found between the given pincodes\"}")))
    @ApiResponse(responseCode = "429", description = "The routing or geocoding provider's rate limit was reached",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"error\": \"Routing service rate limit reached, please try again later\"}")))
    @ApiResponse(responseCode = "502", description = "The routing or geocoding provider failed",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"error\": \"Routing service returned an error (HTTP 500)\"}")))
    @ApiResponse(responseCode = "504", description = "The routing or geocoding provider timed out",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"error\": \"Routing service timed out, please try again\"}")))
    public TollResponse findTollPlazas(@Valid @RequestBody TollRequest request) {
        return service.findTollPlazas(request.sourcePincode(), request.destinationPincode());
    }
}
