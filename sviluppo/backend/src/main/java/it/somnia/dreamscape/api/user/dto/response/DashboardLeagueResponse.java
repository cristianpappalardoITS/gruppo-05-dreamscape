package it.somnia.dreamscape.api.user.dto.response;

// La lega del giocatore e quella successiva; nextLeagueName e nextLeagueMinRating sono null
// per la lega più alta
public record DashboardLeagueResponse(String code, String name, String nextLeagueName,
        Integer nextLeagueMinRating) {
}
