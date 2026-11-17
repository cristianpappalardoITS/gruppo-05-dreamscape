package it.somnia.dreamscape.engine.server.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

// Senza ENGINE_API_KEY il servizio non deve partire: giocherebbe partite per chiunque
class EnginePropertiesTest {

    @Test
    void constructor_emptyKey_throws() {
        // When / Then
        assertThrows(IllegalStateException.class, () -> new EngineProperties(""));
    }

    @Test
    void constructor_blankKey_throws() {
        // When / Then
        assertThrows(IllegalStateException.class, () -> new EngineProperties("   "));
    }

    @Test
    void constructor_withKey_keepsIt() {
        // When
        EngineProperties properties = new EngineProperties("chiave");

        // Then
        assertEquals("chiave", properties.apiKey());
    }
}
