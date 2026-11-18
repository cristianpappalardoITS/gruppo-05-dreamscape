package it.somnia.dreamscape.engine.simulation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SimulationOptionsTest {

    @Test
    void parse_noArgs_usesDefaults() {
        assertEquals(new SimulationOptions(5_000, 87), SimulationOptions.parse(new String[0]));
    }

    @Test
    void parse_bothOptionsInAnyOrder_readsValues() {
        assertEquals(new SimulationOptions(100, 7),
                SimulationOptions.parse(new String[] {"--seed", "7", "--partite", "100"}));
    }

    @Test
    void parse_lastOptionWithoutValue_throws() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> SimulationOptions.parse(new String[] {"--partite", "100", "--seed"}));

        assertTrue(error.getMessage().contains("--seed"));
    }

    @Test
    void parse_singleUnknownOption_throws() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> SimulationOptions.parse(new String[] {"--giochi"}));

        assertTrue(error.getMessage().contains("--giochi"));
    }

    @Test
    void parse_unknownOptionWithValue_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> SimulationOptions.parse(new String[] {"--partite", "100", "--limite", "60"}));
    }

    @Test
    void parse_valueNotANumber_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> SimulationOptions.parse(new String[] {"--partite", "tante"}));
    }

    @Test
    void parse_repeatedOption_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> SimulationOptions.parse(new String[] {"--seed", "1", "--seed", "2"}));
    }

    @Test
    void parse_zeroGames_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> SimulationOptions.parse(new String[] {"--partite", "0"}));
    }
}
