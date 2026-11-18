package it.somnia.dreamscape.api.challenge.dto.response;

import java.util.UUID;

// La sfida è stata giocata: la pagina va su /partite/{matchId}, che legge GET /matches/{matchId}
public record AcceptChallengeResponse(UUID matchId) {
}
