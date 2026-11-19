package com.tollfinder.service;

import com.tollfinder.model.Coordinate;
import com.tollfinder.model.TollPlaza;
import com.tollfinder.repository.TollPlazaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Upsert by location: safe to run on every startup. */
@Service
public class TollPlazaImporter {

    private static final Logger log = LoggerFactory.getLogger(TollPlazaImporter.class);

    public record ImportResult(int inserted, int updated, int unchanged) {
    }

    private final TollPlazaRepository repository;

    public TollPlazaImporter(TollPlazaRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ImportResult upsert(List<TollPlaza> incoming) {
        Map<Coordinate, TollPlaza> unique = dedupeByLocation(incoming);
        Map<Coordinate, TollPlaza> existing = repository.findAll().stream()
                .collect(Collectors.toMap(TollPlaza::location, Function.identity()));

        List<TollPlaza> toSave = new ArrayList<>();
        int inserted = 0, updated = 0, unchanged = 0;
        for (TollPlaza plaza : unique.values()) {
            TollPlaza current = existing.get(plaza.location());
            if (current == null) {
                toSave.add(plaza);
                inserted++;
            } else if (!current.getName().equals(plaza.getName()) || !current.getState().equals(plaza.getState())) {
                current.setName(plaza.getName());
                current.setState(plaza.getState());
                toSave.add(current);
                updated++;
            } else {
                unchanged++;
            }
        }
        repository.saveAll(toSave);
        log.info("Toll plazas: {} inserted, {} updated, {} unchanged", inserted, updated, unchanged);
        return new ImportResult(inserted, updated, unchanged);
    }

    // First row wins when several rows share a location; the rest are logged, not stored.
    private Map<Coordinate, TollPlaza> dedupeByLocation(List<TollPlaza> incoming) {
        Map<Coordinate, TollPlaza> unique = new LinkedHashMap<>();
        for (TollPlaza plaza : incoming) {
            TollPlaza first = unique.putIfAbsent(plaza.location(), plaza);
            if (first != null && !first.getName().equals(plaza.getName())) {
                log.warn("Same location {} has different names, keeping '{}' and ignoring '{}'",
                        plaza.location(), first.getName(), plaza.getName());
            }
        }
        return unique;
    }
}
