package it.somnia.dreamscape.api.league.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import it.somnia.dreamscape.api.league.model.League;

public interface LeagueRepository extends JpaRepository<League, UUID> {

    // Le leghe dalla più bassa alla più alta
    List<League> findAllByOrderByMinRatingAsc();

    // La lega di un punteggio: quella con la soglia più alta che il punteggio raggiunge
    Optional<League> findFirstByMinRatingLessThanEqualOrderByMinRatingDesc(int rating);

    // La lega successiva: la prima con la soglia sopra il punteggio; vuota per la lega più alta
    Optional<League> findFirstByMinRatingGreaterThanOrderByMinRatingAsc(int rating);

    // Le leghe che il punteggio raggiunge e in cui il giocatore non è mai entrato, dalla più bassa. La lega con
    // soglia 0 è quella di partenza: non si raggiunge, ci si è già dalla registrazione
    @Query("""
            SELECT l FROM League l
            WHERE l.minRating > 0
              AND l.minRating <= :rating
              AND NOT EXISTS (SELECT r FROM UserLeagueReward r WHERE r.user.id = :userId AND r.league = l)
            ORDER BY l.minRating ASC
            """)
    List<League> findReachedForTheFirstTime(UUID userId, int rating);
}
