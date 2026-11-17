package it.somnia.dreamscape.api.friend.dto.response;

import java.time.Instant;
import java.util.UUID;

import it.somnia.dreamscape.api.user.dto.response.UserSummaryResponse;

// Una richiesta di amicizia: user è l'altro giocatore (chi l'ha mandata, o a chi l'ho mandata io)
public record FriendRequestResponse(UUID id, UserSummaryResponse user, Instant createdAt) {
}
