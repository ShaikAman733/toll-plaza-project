package com.tollfinder.repository;

import com.tollfinder.model.TollPlaza;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TollPlazaRepository extends JpaRepository<TollPlaza, Long> {

    List<TollPlaza> findByLatitudeBetweenAndLongitudeBetween(double minLat, double maxLat,
                                                             double minLon, double maxLon);
}
