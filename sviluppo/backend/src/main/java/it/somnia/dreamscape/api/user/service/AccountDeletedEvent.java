package it.somnia.dreamscape.api.user.service;

import java.util.UUID;

/**
 * Pubblicato quando un account viene cancellato (reso anonimo). Amici, scambi e sfide lo
 * ascoltano con {@code @EventListener} per togliere le amicizie e annullare quello che è in attesa.
 */
public record AccountDeletedEvent(UUID userId) {
}
