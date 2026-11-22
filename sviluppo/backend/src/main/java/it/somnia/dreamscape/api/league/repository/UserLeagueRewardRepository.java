package it.somnia.dreamscape.api.league.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import it.somnia.dreamscape.api.league.model.UserLeagueReward;

public interface UserLeagueRewardRepository extends JpaRepository<UserLeagueReward, UUID> {

    // Le leghe in cui un giocatore è entrato con una partita, dalla più alta (leagueReached nel risultato)
    @EntityGraph(attributePaths = { "league", "creditTransaction" })
    List<UserLeagueReward> findByUserIdAndMatchIdOrderByLeagueMinRatingDesc(UUID userId, UUID matchId);
}
