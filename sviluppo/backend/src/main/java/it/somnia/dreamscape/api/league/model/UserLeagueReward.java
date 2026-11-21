package it.somnia.dreamscape.api.league.model;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import it.somnia.dreamscape.api.user.model.CreditTransaction;
import it.somnia.dreamscape.api.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// Una lega raggiunta da un giocatore (V10). La riga nasce la prima volta che entra nella lega e resta anche se
// poi scende: per questo il premio arriva una volta sola (decisione 011)
@Entity
@Table(name = "user_league_rewards")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserLeagueReward {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "league_id")
    private League league;

    @CreationTimestamp
    @Column(name = "reached_at", nullable = false, updatable = false)
    private Instant reachedAt;

    // La partita con cui si è entrati nella lega: serve a leagueReached nel risultato della partita
    @Column(name = "match_id")
    private UUID matchId;

    // Il movimento del premio; null se la lega non aveva premio
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_transaction_id")
    private CreditTransaction creditTransaction;

    public UserLeagueReward(User user, League league, UUID matchId, CreditTransaction creditTransaction) {
        this.user = user;
        this.league = league;
        this.matchId = matchId;
        this.creditTransaction = creditTransaction;
    }

    // I Dream Credits presi con questa lega
    public int getRewardCredits() {
        return creditTransaction == null ? 0 : creditTransaction.getAmount();
    }
}
