package it.somnia.dreamscape.api.match.model;

// Il tipo di partita (decisione 010): le regole sono le stesse (BASE), cambia cosa si vince
public enum MatchType {
    // Partita rapida o sfida tra amici: niente rank e niente crediti
    CASUAL,
    // Partita classificata del matchmaking (#127): rank e crediti
    RANKED
}
