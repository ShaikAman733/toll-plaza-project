package com.tollfinder.service;

import com.tollfinder.model.Coordinate;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

class BoundingBoxTest {

    @Test
    void coversAllPointsPlusMargin() {
        BoundingBox box = BoundingBox.around(
                List.of(new Coordinate(12.0, 77.0), new Coordinate(18.0, 73.0), new Coordinate(15.0, 75.0)), 1_000);

        // 1 km is about 0.009 degrees of latitude
        assertThat(box.minLat()).isCloseTo(12.0 - 0.009, offset(0.0005));
        assertThat(box.maxLat()).isCloseTo(18.0 + 0.009, offset(0.0005));
        assertThat(box.minLon()).isLessThan(73.0 - 0.009);
        assertThat(box.maxLon()).isGreaterThan(77.0 + 0.009);
    }
}
