package it.somnia.dreamscape.api.user.repository;

// Una lega letta dalla tabella leagues: solo i campi che servono al riepilogo della Dashboard
public interface LeagueView {

    String getCode();

    String getName();

    int getMinRating();
}
