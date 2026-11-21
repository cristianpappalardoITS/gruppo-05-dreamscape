package it.somnia.dreamscape.api.admin.dto.response;

import it.somnia.dreamscape.api.match.model.GameMode;

/**
 * Una modalità di gioco nel backoffice ({@code /admin/game-modes}). Oggi c'è solo BASE, sempre attiva.
 *
 * @param code               codice fisso, es. BASE
 * @param name               nome
 * @param description        descrizione
 * @param startingLifePoints punti vita iniziali di ogni giocatore
 * @param roundLimit         round dopo cui la partita finisce in pareggio
 * @param isActive           se si gioca
 */
public record AdminGameModeResponse(String code, String name, String description, int startingLifePoints,
        int roundLimit, boolean isActive) {

    public static AdminGameModeResponse from(GameMode mode) {
        return new AdminGameModeResponse(mode.getCode(), mode.getName(), mode.getDescription(),
                mode.getStartingLifePoints(), mode.getRoundLimit(), mode.isActive());
    }
}
