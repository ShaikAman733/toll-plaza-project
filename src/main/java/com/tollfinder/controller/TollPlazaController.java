package com.tollfinder.controller;

import com.tollfinder.model.TollRequest;
import com.tollfinder.model.TollResponse;
import com.tollfinder.service.TollPlazaService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/toll-plazas")
public class TollPlazaController {

    private final TollPlazaService service;

    public TollPlazaController(TollPlazaService service) {
        this.service = service;
    }

    @PostMapping
    public TollResponse findTollPlazas(@Valid @RequestBody TollRequest request) {
        return service.findTollPlazas(request.sourcePincode(), request.destinationPincode());
    }
}
