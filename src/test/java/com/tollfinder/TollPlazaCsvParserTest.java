package com.tollfinder;

import com.tollfinder.model.TollPlaza;
import com.tollfinder.service.TollPlazaCsvParser;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TollPlazaCsvParserTest {

    private final TollPlazaCsvParser parser = new TollPlazaCsvParser();

    private List<TollPlaza> parse(String rows) throws Exception {
        return parser.parse(new StringReader("longitude,latitude,toll_name,geo_state\r\n" + rows));
    }

    @Test
    void parsesRowsWithWindowsLineEndings() throws Exception {
        List<TollPlaza> plazas = parse("74.99,23.41,Dhamnod Toll Plaza,Madhya Pradesh\r\n79.61,22.07,Seoni Toll Plaza,Madhya Pradesh\r\n");

        assertThat(plazas).hasSize(2);
        assertThat(plazas.get(0).getName()).isEqualTo("Dhamnod Toll Plaza");
        assertThat(plazas.get(0).getLatitude()).isEqualTo(23.41);
        assertThat(plazas.get(0).getLongitude()).isEqualTo(74.99);
        assertThat(plazas.get(0).getState()).isEqualTo("Madhya Pradesh");
    }

    @Test
    void missingNamesGetFallbackWithState() throws Exception {
        List<TollPlaza> plazas = parse("82.9,21.2,nan,Chhattisgarh\r\n80.1,20.1,,Kerala\r\n80.2,20.2,NULL,Goa\r\n");

        assertThat(plazas).extracting(TollPlaza::getName).containsExactly(
                "Unnamed Toll Plaza (Chhattisgarh)", "Unnamed Toll Plaza (Kerala)", "Unnamed Toll Plaza (Goa)");
    }

    @Test
    void handlesQuotedNamesWithCommas() throws Exception {
        List<TollPlaza> plazas = parse("77.1,28.1,\"Foo, Bar Toll Plaza\",Delhi\r\n");

        assertThat(plazas.get(0).getName()).isEqualTo("Foo, Bar Toll Plaza");
    }

    @Test
    void skipsRowsWithInvalidCoordinates() throws Exception {
        List<TollPlaza> plazas = parse("abc,28.1,Bad,Delhi\r\n77.1,28.1,Good,Delhi\r\n");

        assertThat(plazas).extracting(TollPlaza::getName).containsExactly("Good");
    }
}
