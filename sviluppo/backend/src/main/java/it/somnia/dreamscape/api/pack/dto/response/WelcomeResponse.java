package it.somnia.dreamscape.api.pack.dto.response;

import java.util.List;

// Pagina Benvenuto (guida ai dati, "Benvenuto"). starterFaction null = fazione non ancora scelta:
// in quel caso le due liste sono vuote.
public record WelcomeResponse(String starterFaction, List<OpenedCardResponse> starterCards,
        List<WelcomeOpeningResponse> welcomeOpenings) {

    public static WelcomeResponse notClaimed() {
        return new WelcomeResponse(null, List.of(), List.of());
    }
}
