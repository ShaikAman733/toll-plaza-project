package com.tollfinder;

import com.tollfinder.model.TollPlaza;
import com.tollfinder.repository.TollPlazaRepository;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Runs the "prod" profile against a real PostgreSQL, the way Render + Neon will. */
@SpringBootTest
@ActiveProfiles("prod")
class PostgresProdProfileTest {

    private static EmbeddedPostgres postgres;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) throws IOException {
        postgres = EmbeddedPostgres.start();
        // the same variables the prod profile reads on Render
        registry.add("DB_URL", () -> postgres.getJdbcUrl("postgres", "postgres"));
        registry.add("DB_USERNAME", () -> "postgres");
        registry.add("DB_PASSWORD", () -> "postgres");
    }

    @AfterAll
    static void stopDatabase() throws IOException {
        postgres.close();
    }

    @Autowired TollPlazaRepository repository;
    @Autowired JdbcTemplate jdbc;

    @Test
    void tableIsCreatedAndCsvIsLoaded() {
        assertThat(repository.count()).isEqualTo(1513);
    }

    @Test
    void uniqueConstraintOnLatitudeAndLongitudeExists() {
        List<String> columns = jdbc.queryForList("""
                select kcu.column_name from information_schema.table_constraints tc
                join information_schema.key_column_usage kcu on tc.constraint_name = kcu.constraint_name
                where tc.table_name = 'toll_plaza' and tc.constraint_type = 'UNIQUE'
                order by kcu.ordinal_position""", String.class);

        assertThat(columns).containsExactlyInAnyOrder("latitude", "longitude");
    }

    @Test
    void databaseRejectsASecondPlazaAtTheSameLocation() {
        TollPlaza existing = repository.findAll().get(0);

        assertThatThrownBy(() -> repository.saveAndFlush(
                new TollPlaza("Duplicate", existing.getState(), existing.getLatitude(), existing.getLongitude())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void boundingBoxQueryWorksOnPostgres() {
        // box around Bengaluru to Hosur: contains at least the Kadathanamale-side plazas
        assertThat(repository.findByLatitudeBetweenAndLongitudeBetween(13.0, 13.4, 77.0, 77.7)).isNotEmpty();
    }
}
