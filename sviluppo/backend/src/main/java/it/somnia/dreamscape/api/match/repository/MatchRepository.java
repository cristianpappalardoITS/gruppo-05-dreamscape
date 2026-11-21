package it.somnia.dreamscape.api.match.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import it.somnia.dreamscape.api.match.model.Match;
import it.somnia.dreamscape.api.match.model.MatchType;

public interface MatchRepository extends JpaRepository<Match, UUID> {

    // Il risultato di una partita con i due giocatori, per GET /matches/{matchId}
    @EntityGraph(attributePaths = { "challenger", "opponent", "winner" })
    Optional<Match> findWithPlayersById(UUID matchId);

    // Le partite tra due giocatori, in tutti e due i ruoli, giocate in [from, to) che hanno cambiato il
    // punteggio: servono al limite di 3 classificate al giorno contro lo stesso avversario (RankService)
    @Query("""
            SELECT COUNT(m) FROM Match m
            WHERE m.matchType = :matchType
              AND m.challengerRatingChange IS NOT NULL
              AND ((m.challenger.id = :firstUserId AND m.opponent.id = :secondUserId)
                OR (m.challenger.id = :secondUserId AND m.opponent.id = :firstUserId))
              AND m.playedAt >= :from AND m.playedAt < :to
            """)
    long countRatedBetween(MatchType matchType, UUID firstUserId, UUID secondUserId, Instant from, Instant to);

    // Le partite di un giocatore, in tutti e due i ruoli, giocate in [from, to) che hanno cambiato il punteggio:
    // il matchmaking preferisce gli avversari con cui il giocatore non ha già raggiunto il limite di oggi
    @Query("""
            SELECT m FROM Match m
            WHERE m.matchType = :matchType
              AND m.challengerRatingChange IS NOT NULL
              AND (m.challenger.id = :userId OR m.opponent.id = :userId)
              AND m.playedAt >= :from AND m.playedAt < :to
            """)
    List<Match> findRatedInvolving(MatchType matchType, UUID userId, Instant from, Instant to);

    // Storico (GET /me/matches): le partite di un giocatore, in tutti e due i ruoli, dalla più recente
    @EntityGraph(attributePaths = { "challenger", "opponent", "winner" })
    @Query("""
            SELECT m FROM Match m
            WHERE m.challenger.id = :userId OR m.opponent.id = :userId
            ORDER BY m.playedAt DESC, m.id DESC
            """)
    Page<Match> findPlayedBy(UUID userId, Pageable pageable);

    // Come findPlayedBy, solo le partite di un tipo
    @EntityGraph(attributePaths = { "challenger", "opponent", "winner" })
    @Query("""
            SELECT m FROM Match m
            WHERE (m.challenger.id = :userId OR m.opponent.id = :userId) AND m.matchType = :matchType
            ORDER BY m.playedAt DESC, m.id DESC
            """)
    Page<Match> findPlayedBy(UUID userId, MatchType matchType, Pageable pageable);
}
