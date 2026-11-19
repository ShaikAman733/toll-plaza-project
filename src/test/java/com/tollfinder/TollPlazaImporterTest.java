package com.tollfinder;

import com.tollfinder.model.TollPlaza;
import com.tollfinder.repository.TollPlazaRepository;
import com.tollfinder.service.TollPlazaImporter;
import com.tollfinder.service.TollPlazaImporter.ImportResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TollPlazaImporter.class)
class TollPlazaImporterTest {

    @Autowired TollPlazaImporter importer;
    @Autowired TollPlazaRepository repository;

    private TollPlaza plaza(String name, double lat, double lon) {
        return new TollPlaza(name, "Karnataka", lat, lon);
    }

    @Test
    void exactDuplicatesAreStoredOnce() {
        ImportResult result = importer.upsert(List.of(plaza("A", 1.0, 2.0), plaza("A", 1.0, 2.0), plaza("B", 3.0, 4.0)));

        assertThat(result.inserted()).isEqualTo(2);
        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    void firstNameWinsWhenOneLocationHasTwoNames() {
        importer.upsert(List.of(plaza("First", 1.0, 2.0), plaza("Second", 1.0, 2.0)));

        assertThat(repository.findAll()).extracting(TollPlaza::getName).containsExactly("First");
    }

    @Test
    void sameNameAtDifferentLocationsKeepsBoth() {
        importer.upsert(List.of(plaza("Same", 1.0, 2.0), plaza("Same", 5.0, 6.0)));

        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    void runningTwiceDoesNotDuplicateRows() {
        List<TollPlaza> data = List.of(plaza("A", 1.0, 2.0), plaza("B", 3.0, 4.0));
        importer.upsert(data);

        ImportResult second = importer.upsert(data);

        assertThat(second).isEqualTo(new ImportResult(0, 0, 2));
        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    void changedNameIsUpdatedInPlace() {
        importer.upsert(List.of(plaza("Old", 1.0, 2.0)));

        ImportResult result = importer.upsert(List.of(plaza("New", 1.0, 2.0)));

        assertThat(result.updated()).isEqualTo(1);
        assertThat(repository.findAll()).extracting(TollPlaza::getName).containsExactly("New");
    }
}
