package it.somnia.dreamscape.api.pack.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class PackTest {

    private static final Instant NOW = Instant.parse("2026-11-10T12:00:00Z");

    @Test
    void isOnSale_activeShopPackWithoutPeriod_returnsTrue() {
        // Given
        Pack pack = new Pack();

        // When / Then
        assertTrue(pack.isOnSale(NOW));
    }

    @Test
    void isOnSale_welcomePack_returnsFalse() {
        // Given: i pacchetti di benvenuto si regalano, non si comprano
        Pack pack = new Pack();
        pack.setPackType(PackType.WELCOME);

        // When / Then
        assertFalse(pack.isOnSale(NOW));
    }

    @Test
    void isOnSale_inactivePack_returnsFalse() {
        // Given
        Pack pack = new Pack();
        pack.setActive(false);

        // When / Then
        assertFalse(pack.isOnSale(NOW));
    }

    @Test
    void isOnSale_saleStartsNow_returnsTrue() {
        // Given: available_from è compreso nel periodo
        Pack pack = new Pack();
        pack.setAvailableFrom(NOW);

        // When / Then
        assertTrue(pack.isOnSale(NOW));
    }

    @Test
    void isOnSale_saleEndsNow_returnsFalse() {
        // Given: available_until non è compreso nel periodo
        Pack pack = new Pack();
        pack.setAvailableUntil(NOW);

        // When / Then
        assertFalse(pack.isOnSale(NOW));
    }
}
