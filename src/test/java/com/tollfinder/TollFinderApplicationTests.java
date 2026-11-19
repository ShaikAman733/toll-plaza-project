package com.tollfinder;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "toll.load-on-startup=false")
class TollFinderApplicationTests {

    @Test
    void contextLoads() {
    }
}
