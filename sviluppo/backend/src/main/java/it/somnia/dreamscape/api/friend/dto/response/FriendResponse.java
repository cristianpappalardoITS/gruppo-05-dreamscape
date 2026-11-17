package it.somnia.dreamscape.api.friend.dto.response;

import java.time.Instant;

import it.somnia.dreamscape.api.user.dto.response.UserSummaryResponse;

// Un amico: { "user": { "id", "username" }, "since": quando è stata accettata l'amicizia }
public record FriendResponse(UserSummaryResponse user, Instant since) {
}
