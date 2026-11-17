package it.somnia.dreamscape.api.friend.service;

import java.util.UUID;

/**
 * Pubblicato quando due giocatori smettono di essere amici: uno toglie l'altro ({@code DELETE /friends/{userId}})
 * oppure uno dei due cancella l'account. Chi lo ascolta con {@code @EventListener}, nella stessa transazione,
 * annulla quello che era in attesa tra i due (decisione 026): gli scambi (#93) e le sfide (#94).
 *
 * @param userId         chi ha tolto l'amicizia o ha cancellato l'account
 * @param formerFriendId l'altro giocatore
 */
public record FriendshipRemovedEvent(UUID userId, UUID formerFriendId) {
}
