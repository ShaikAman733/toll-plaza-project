package com.tollfinder.service;

import com.tollfinder.model.TollPlaza;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class TollPlazaCsvParser {

    private static final Logger log = LoggerFactory.getLogger(TollPlazaCsvParser.class);
    // the source file has the literal text "nan" where a name is missing
    private static final Set<String> MISSING_NAME_VALUES = Set.of("", "nan", "null");

    public List<TollPlaza> parse(Reader reader) throws IOException {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader().setSkipHeaderRecord(true).setTrim(true).setIgnoreEmptyLines(true).build();
        List<TollPlaza> plazas = new ArrayList<>();
        try (CSVParser parser = format.parse(reader)) {
            for (CSVRecord row : parser) {
                try {
                    plazas.add(toPlaza(row));
                } catch (RuntimeException e) {
                    log.warn("Skipping CSV line {}: {}", row.getRecordNumber() + 1, e.getMessage());
                }
            }
        }
        return plazas;
    }

    private TollPlaza toPlaza(CSVRecord row) {
        String state = row.get("geo_state");
        String name = row.get("toll_name");
        if (MISSING_NAME_VALUES.contains(name.toLowerCase())) {
            name = "Unnamed Toll Plaza (" + state + ")";
        }
        return new TollPlaza(name, state,
                Double.parseDouble(row.get("latitude")), Double.parseDouble(row.get("longitude")));
    }
}
