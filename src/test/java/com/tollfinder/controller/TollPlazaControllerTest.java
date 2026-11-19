package com.tollfinder.controller;

import com.tollfinder.exception.ExternalServiceException;
import com.tollfinder.exception.InvalidPincodeException;
import com.tollfinder.exception.RouteNotFoundException;
import com.tollfinder.exception.SamePincodeException;
import com.tollfinder.model.TollResponse;
import com.tollfinder.model.TollResponse.RouteInfo;
import com.tollfinder.model.TollResponse.TollPlazaInfo;
import com.tollfinder.service.TollPlazaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TollPlazaController.class)
class TollPlazaControllerTest {

    private static final String URL = "/api/v1/toll-plazas";

    @Autowired MockMvc mvc;
    @MockitoBean TollPlazaService service;

    private ResultActions postJson(String json) throws Exception {
        return mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions postPincodes(String source, String destination) throws Exception {
        return postJson("{\"sourcePincode\":\"" + source + "\",\"destinationPincode\":\"" + destination + "\"}");
    }

    @Test
    void returnsRouteAndTollPlazas() throws Exception {
        when(service.findTollPlazas("560064", "411045")).thenReturn(new TollResponse(
                new RouteInfo("560064", "411045", 842.5),
                List.of(new TollPlazaInfo("Hosur Toll Plaza", 12.74, 77.83, 41.2))));

        postPincodes("560064", "411045")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.route.sourcePincode").value("560064"))
                .andExpect(jsonPath("$.route.destinationPincode").value("411045"))
                .andExpect(jsonPath("$.route.distanceInKm").value(842.5))
                .andExpect(jsonPath("$.tollPlazas[0].name").value("Hosur Toll Plaza"))
                .andExpect(jsonPath("$.tollPlazas[0].latitude").value(12.74))
                .andExpect(jsonPath("$.tollPlazas[0].longitude").value(77.83))
                .andExpect(jsonPath("$.tollPlazas[0].distanceFromSource").value(41.2));
    }

    @Test
    void noPlazasReturnsRouteWithEmptyList() throws Exception {
        when(service.findTollPlazas("110001", "560001")).thenReturn(
                new TollResponse(new RouteInfo("110001", "560001", 2100), List.of()));

        postPincodes("110001", "560001")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.route.distanceInKm").value(2100))
                .andExpect(jsonPath("$.tollPlazas").isEmpty());
    }

    @Test
    void badPincodeFormatsAreRejectedWithTheExactMessage() throws Exception {
        for (String bad : List.of("12345", "1234567", "abcdef", "012345", "", " 560064")) {
            postPincodes(bad, "411045")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Invalid source or destination pincode"));
            postPincodes("560064", bad)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Invalid source or destination pincode"));
        }
    }

    @Test
    void missingFieldsAreRejected() throws Exception {
        postJson("{\"sourcePincode\":\"560064\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid source or destination pincode"));
        postJson("{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid source or destination pincode"));
    }

    @Test
    void numericPincodesInJsonAreAcceptedAsStrings() throws Exception {
        when(service.findTollPlazas("560064", "411045")).thenReturn(
                new TollResponse(new RouteInfo("560064", "411045", 1), List.of()));

        postJson("{\"sourcePincode\":560064,\"destinationPincode\":411045}").andExpect(status().isOk());
    }

    @Test
    void malformedJsonAndMissingBodyGive400() throws Exception {
        postJson("{not json").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Request body must be JSON with sourcePincode and destinationPincode"));
        mvc.perform(post(URL)).andExpect(status().isBadRequest());
    }

    @Test
    void unknownPincodeGives400() throws Exception {
        when(service.findTollPlazas("999999", "411045")).thenThrow(new InvalidPincodeException());

        postPincodes("999999", "411045")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid source or destination pincode"));
    }

    @Test
    void samePincodeGives400() throws Exception {
        when(service.findTollPlazas("560064", "560064")).thenThrow(new SamePincodeException());

        postPincodes("560064", "560064")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Source and destination pincodes cannot be the same"));
    }

    @Test
    void noRouteGives422() throws Exception {
        when(service.findTollPlazas("560064", "411045")).thenThrow(new RouteNotFoundException());

        postPincodes("560064", "411045").andExpect(status().isUnprocessableEntity());
    }

    @Test
    void providerFailuresKeepTheirStatusAndShowAClearMessage() throws Exception {
        when(service.findTollPlazas("560064", "411045")).thenThrow(
                new ExternalServiceException(HttpStatus.TOO_MANY_REQUESTS, "Routing service rate limit reached, please try again later"));

        postPincodes("560064", "411045")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("Routing service rate limit reached, please try again later"));
    }

    @Test
    void unexpectedErrorsGiveGeneric500WithoutDetails() throws Exception {
        when(service.findTollPlazas("560064", "411045")).thenThrow(new IllegalStateException("db password is hunter2"));

        postPincodes("560064", "411045")
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Something went wrong, please try again later"));
    }

    @Test
    void otherHttpMethodsGet405InTheSameErrorShape() throws Exception {
        mvc.perform(get(URL))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.error").value("Method Not Allowed"));
    }

    @Test
    void wrongContentTypeGets415() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.TEXT_PLAIN).content("x"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.error").value("Unsupported Media Type"));
    }
}
