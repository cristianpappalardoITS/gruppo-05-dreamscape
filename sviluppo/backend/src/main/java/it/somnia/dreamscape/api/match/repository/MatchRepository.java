package it.somnia.dreamscape.api.match.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import it.somnia.dreamscape.api.match.model.Match;

public interface MatchRepository extends JpaRepository<Match, UUID> {

    // Il risultato di una partita con i due giocatori, per GET /matches/{matchId}
    @EntityGraph(attributePaths = { "challenger", "opponent", "winner" })
    Optional<Match> findWithPlayersById(UUID matchId);
}
