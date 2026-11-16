package it.somnia.dreamscape.api.user.dto.response;

import java.util.List;

import it.somnia.dreamscape.api.mission.dto.response.MissionResponse;
import it.somnia.dreamscape.api.pack.dto.response.OpenedCardResponse;

// Il riepilogo della Dashboard (GET /me/summary). È anche il modo per avere i crediti
// dopo un ricaricamento della pagina. latestCards sono le carte dell'ultima apertura.
public record DashboardResponse(
        String username,
        int credits,
        int rating,
        DashboardLeagueResponse league,
        boolean hasRankedDeck,
        List<MissionResponse> missions,
        int pendingFriendRequests,
        int pendingTrades,
        int pendingChallenges,
        List<OpenedCardResponse> latestCards) {
}
