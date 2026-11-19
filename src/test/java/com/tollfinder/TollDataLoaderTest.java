package com.tollfinder;

import com.tollfinder.repository.TollPlazaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/** Loads the real CSV that ships with the app. */
@SpringBootTest
class TollDataLoaderTest {

    @Autowired TollPlazaRepository repository;

    @Test
    void startupLoadsOnlyUniqueLocationsFromCsv() {
        assertThat(repository.count()).isEqualTo(1513);
        assertThat(repository.findAll()).noneMatch(p -> p.getName().equalsIgnoreCase("nan"));
    }
}
