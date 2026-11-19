package com.tollfinder.config;

import com.tollfinder.model.TollPlaza;
import com.tollfinder.service.TollPlazaCsvParser;
import com.tollfinder.service.TollPlazaImporter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@ConditionalOnProperty(name = "toll.load-on-startup", havingValue = "true", matchIfMissing = true)
public class TollDataLoader implements ApplicationRunner {

    private final TollPlazaCsvParser parser;
    private final TollPlazaImporter importer;
    private final Resource csv;

    public TollDataLoader(TollPlazaCsvParser parser, TollPlazaImporter importer,
                          @Value("${toll.csv-path}") Resource csv) {
        this.parser = parser;
        this.importer = importer;
        this.csv = csv;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Reader reader = new InputStreamReader(csv.getInputStream(), StandardCharsets.UTF_8)) {
            List<TollPlaza> plazas = parser.parse(reader);
            importer.upsert(plazas);
        }
    }
}
